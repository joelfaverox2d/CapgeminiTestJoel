package com.capgemini.test.code.mapper;

import com.capgemini.test.code.api.dto.RoomDto;
import com.capgemini.test.code.entity.Room;
import org.springframework.stereotype.Component;


@Component
public class RoomMapper {

  public Room toEntity(RoomDto dto) {
    if (dto == null) {
      return null;
    }
    Room room = new Room();
    room.setId(dto.getId());
    room.setName(dto.getName());
    return room;
  }

  public RoomDto toDto(Room room) {
    if (room == null) {
      return null;
    }
    return new RoomDto()
        .id(room.getId())
        .name(room.getName());
  }
}
