package com.capgemini.test.code.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.capgemini.test.code.entity.Room;
import com.capgemini.test.code.entity.User;
import com.capgemini.test.code.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integración entre el repositorio JPA y una base de datos real (H2 en memoria).
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  private User persistUser(Room room, String email, String dni, String phone) {
    User user = new User();
    user.setName("pablo");
    user.setEmail(email);
    user.setDni(dni);
    user.setPhone(phone);
    user.setRole("admin");
    user.setRoom(room);
    return entityManager.persistAndFlush(user);
  }

  @Test
  void existsByEmail_returnsTrueOnlyForStoredEmail() {
    Room room1 = entityManager.find(Room.class, 1L);
    persistUser(room1, "pablo@email.com", "11111111A", "600000001");

    assertThat(userRepository.existsByEmail("pablo@email.com")).isTrue();
    assertThat(userRepository.existsByEmail("otro@email.com")).isFalse();
  }

  @Test
  void findByIdAndRoomId_onlyFindsUsersOfThatRoom() {
    Room room1 = entityManager.find(Room.class, 1L);
    Room room2 = new Room();
    room2.setName("Sala test");
    entityManager.persistAndFlush(room2);

    User inRoom1 = persistUser(room1, "a@email.com", "11111111A", "600000001");
    User inRoom2 = persistUser(room2, "b@email.com", "22222222B", "600000002");

    assertThat(userRepository.findByIdAndRoomId(inRoom1.getId(), 1L)).isPresent();
    assertThat(userRepository.findByIdAndRoomId(inRoom2.getId(), 1L)).isEmpty();
  }
}
