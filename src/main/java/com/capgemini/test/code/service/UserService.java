package com.capgemini.test.code.service;

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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private static final Long DEFAULT_ROOM_ID = 1L;
  private static final String ROLE_ADMIN = "admin";
  private static final String ROLE_SUPERADMIN = "superadmin";
  private static final String NOTIFICATION_MESSAGE = "usuario guardado";

  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final DniClient dniClient;
  private final NotificationClient notificationClient;

  @Transactional
  public Long createUser(User user) {
    validate(user);
    checkDni(user.getDni());

    Room room = roomRepository.findById(DEFAULT_ROOM_ID)
        .orElseThrow(() -> new IllegalStateException("Room 1 does not exist"));

    user.setId(null);
    user.setRole(user.getRole().toLowerCase());
    user.setRoom(room);

    User saved = userRepository.save(user);

    notify(saved);

    return saved.getId();
  }

  @Transactional(readOnly = true)
  public User getUser(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
  }

  @Transactional
  public User changeRoom(Long userId, Long roomId) {
    if (roomId == null) {
      throw new ValidationException("roomId");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));

    user.setRoom(room);
    return userRepository.save(user);
  }

  private void validate(User request) {
    String name = request.getName();
    if (name == null || name.isBlank() || name.length() > 6) {
      throw new ValidationException("userName");
    }

    String email = request.getEmail();
    if (email == null || !email.contains("@") || !email.contains(".")) {
      throw new ValidationException("email");
    }

    String rol = request.getRole();
    if (!(ROLE_ADMIN.equalsIgnoreCase(rol) || ROLE_SUPERADMIN.equalsIgnoreCase(rol))) {
      throw new ValidationException("rol");
    }

    if (request.getDni() == null || request.getDni().isBlank()) {
      throw new ValidationException("dni");
    }

    if (request.getPhone() == null || request.getPhone().isBlank()) {
      throw new ValidationException("phone");
    }

    if (userRepository.existsByEmail(email)) {
      throw new ValidationException("email");
    }
  }

  private void checkDni(String dni) {
    try {
      dniClient.check(new CheckDniRequest(dni));
    } catch (FeignException.Conflict e) {
      throw new ValidationException("dni");
    } catch (FeignException e) {
      throw new ExternalServiceException("dni validation service unavailable");
    }
  }

  private void notify(User user) {
    if (ROLE_ADMIN.equals(user.getRole())) {
      notificationClient.sendEmail(new EmailRequest(user.getEmail(), NOTIFICATION_MESSAGE));
    } else if (ROLE_SUPERADMIN.equals(user.getRole())) {
      notificationClient.sendSms(new SmsRequest(user.getPhone(), NOTIFICATION_MESSAGE));
    }
  }
}
