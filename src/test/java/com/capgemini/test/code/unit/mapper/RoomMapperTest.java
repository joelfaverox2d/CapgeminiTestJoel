package com.capgemini.test.code.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.capgemini.test.code.api.dto.RoomDto;
import com.capgemini.test.code.entity.Room;
import com.capgemini.test.code.mapper.RoomMapper;
import org.junit.jupiter.api.Test;

class RoomMapperTest {

  private final RoomMapper mapper = new RoomMapper();

  @Test
  void toEntityAndBack_keepsIdAndName() {
    RoomDto dto = new RoomDto().id(1L).name("Sala 1");

    Room room = mapper.toEntity(dto);
    RoomDto back = mapper.toDto(room);

    assertThat(room.getId()).isEqualTo(1L);
    assertThat(room.getName()).isEqualTo("Sala 1");
    assertThat(back.getId()).isEqualTo(1L);
    assertThat(back.getName()).isEqualTo("Sala 1");
  }

  @Test
  void nullInput_returnsNull() {
    assertThat(mapper.toEntity(null)).isNull();
    assertThat(mapper.toDto(null)).isNull();
  }
}
