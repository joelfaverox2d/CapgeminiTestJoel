package com.capgemini.test.code.repository;

import com.capgemini.test.code.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
