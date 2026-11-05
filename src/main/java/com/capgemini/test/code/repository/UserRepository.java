package com.capgemini.test.code.repository;

import com.capgemini.test.code.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByEmail(String email);

  Optional<User> findByIdAndRoomId(Long id, Long roomId);
  Optional<User> findById(Long id);
}
