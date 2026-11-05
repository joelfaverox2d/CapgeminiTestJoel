package com.capgemini.test.code.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capgemini.test.code.clients.CheckDniRequest;
import com.capgemini.test.code.clients.DniClient;
import com.capgemini.test.code.clients.EmailRequest;
import com.capgemini.test.code.clients.NotificationClient;
import com.capgemini.test.code.clients.SmsRequest;
import com.capgemini.test.code.entity.Room;
import com.capgemini.test.code.entity.User;
import com.capgemini.test.code.exception.ExternalServiceException;
import com.capgemini.test.code.exception.RoomNotFoundException;
import com.capgemini.test.code.exception.UserNotFoundException;
import com.capgemini.test.code.exception.ValidationException;
import com.capgemini.test.code.repository.RoomRepository;
import com.capgemini.test.code.repository.UserRepository;
import com.capgemini.test.code.service.UserService;
import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de UserService: todas las dependencias son mocks.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private RoomRepository roomRepository;
  @Mock
  private DniClient dniClient;
  @Mock
  private NotificationClient notificationClient;

  @InjectMocks
  private UserService userService;

  private static User user(String name, String email, String role, String dni) {
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setPhone("677998899");
    user.setRole(role);
    user.setDni(dni);
    return user;
  }

  // Excepciones reales de Feign (no mocks) para simular las respuestas de la API externa
  private static final Request DNI_REQUEST = Request.create(
      Request.HttpMethod.PATCH, "/check-dni", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);

  private static FeignException conflict() {
    return new FeignException.Conflict("Invalid DNI", DNI_REQUEST, null, null);
  }

  private static FeignException serviceUnavailable() {
    return new FeignException.ServiceUnavailable("Service unavailable", DNI_REQUEST, null, null);
  }

  private void givenRoom1AndSaveReturnsId(Long id) {
    Room room = new Room();
    room.setId(1L);
    room.setName("Sala 1");
    when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      saved.setId(id);
      return saved;
    });
  }

  @Test
  void createUser_admin_savesInRoom1AndSendsEmail() {
    givenRoom1AndSaveReturnsId(10L);

    Long id = userService.createUser(user("pablo", "pablo@email.com", "admin", "23454234W"));

    assertThat(id).isEqualTo(10L);

    ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(savedUser.capture());
    assertThat(savedUser.getValue().getRoom().getId()).isEqualTo(1L);

    ArgumentCaptor<EmailRequest> email = ArgumentCaptor.forClass(EmailRequest.class);
    verify(notificationClient).sendEmail(email.capture());
    assertThat(email.getValue().getEmail()).isEqualTo("pablo@email.com");
    assertThat(email.getValue().getMessage()).isEqualTo("usuario guardado");
    verify(notificationClient, never()).sendSms(any());
  }

  @Test
  void createUser_superadmin_sendsSms() {
    givenRoom1AndSaveReturnsId(11L);

    userService.createUser(user("ana", "ana@email.com", "SUPERADMIN", "11111111A"));

    ArgumentCaptor<SmsRequest> sms = ArgumentCaptor.forClass(SmsRequest.class);
    verify(notificationClient).sendSms(sms.capture());
    assertThat(sms.getValue().getPhone()).isEqualTo("677998899");
    assertThat(sms.getValue().getMessage()).isEqualTo("usuario guardado");
    verify(notificationClient, never()).sendEmail(any());
  }

  @Test
  void createUser_nameLongerThan6_throwsUserNameError() {
    assertThatThrownBy(() ->
        userService.createUser(user("pablito", "pablo@email.com", "admin", "23454234W")))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error validation userName");

    verify(dniClient, never()).check(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void createUser_emailWithoutAtOrDot_throwsEmailError() {
    assertThatThrownBy(() ->
        userService.createUser(user("pablo", "pablo-email", "admin", "23454234W")))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error validation email");
  }

  @Test
  void createUser_invalidRole_throwsRolError() {
    assertThatThrownBy(() ->
        userService.createUser(user("pablo", "pablo@email.com", "user", "23454234W")))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error validation rol");
  }

  @Test
  void createUser_emailAlreadyExists_throwsEmailError() {
    when(userRepository.existsByEmail("pablo@email.com")).thenReturn(true);

    assertThatThrownBy(() ->
        userService.createUser(user("pablo", "pablo@email.com", "admin", "23454234W")))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error validation email");

    verify(userRepository, never()).save(any());
  }

  @Test
  void createUser_dniRejectedByExternalApi_throwsDniErrorAndDoesNotSave() {
    when(dniClient.check(any(CheckDniRequest.class))).thenThrow(conflict());

    assertThatThrownBy(() ->
        userService.createUser(user("pablo", "pablo@email.com", "admin", "99999999w")))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error validation dni");

    verify(userRepository, never()).save(any());
    verify(notificationClient, never()).sendEmail(any());
  }

  @Test
  void createUser_dniServiceUnavailable_throwsExternalServiceException() {
    when(dniClient.check(any(CheckDniRequest.class))).thenThrow(serviceUnavailable());

    assertThatThrownBy(() ->
        userService.createUser(user("pablo", "pablo@email.com", "admin", "23454234W")))
        .isInstanceOf(ExternalServiceException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void getUser_existsInRoom1_returnsUser() {
    User stored = user("pablo", "pablo@email.com", "admin", "23454234W");
    stored.setId(5L);
    when(userRepository.findByIdAndRoomId(5L, 1L)).thenReturn(Optional.of(stored));

    User result = userService.getUser(5L);

    assertThat(result.getEmail()).isEqualTo("pablo@email.com");
  }

  @Test
  void getUser_notFound_throwsUserNotFoundException() {
    when(userRepository.findByIdAndRoomId(99L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(99L))
        .isInstanceOf(UserNotFoundException.class);
  }

  // ---------- changeRoom ----------

  @Test
  void changeRoom_existingUserAndRoom_movesUser() {
    User stored = user("pablo", "pablo@email.com", "admin", "23454234W");
    stored.setId(5L);
    Room room2 = new Room();
    room2.setId(2L);
    when(userRepository.findById(5L)).thenReturn(Optional.of(stored));
    when(roomRepository.findById(2L)).thenReturn(Optional.of(room2));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.changeRoom(5L, 2L);

    assertThat(result.getRoom().getId()).isEqualTo(2L);
    verify(userRepository).save(stored);
  }

  @Test
  void changeRoom_roomDoesNotExist_throwsRoomNotFound() {
    User stored = user("pablo", "pablo@email.com", "admin", "23454234W");
    when(userRepository.findById(5L)).thenReturn(Optional.of(stored));
    when(roomRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.changeRoom(5L, 999L))
        .isInstanceOf(RoomNotFoundException.class)
        .hasMessage("room not found 999");

    verify(userRepository, never()).save(any());
  }

  @Test
  void changeRoom_userDoesNotExist_throwsUserNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.changeRoom(99L, 2L))
        .isInstanceOf(UserNotFoundException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void changeRoom_withoutRoomId_throwsValidationError() {
    assertThatThrownBy(() -> userService.changeRoom(5L, null))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error validation roomId");
  }
}
