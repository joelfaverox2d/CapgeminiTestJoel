package com.capgemini.test.code.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.capgemini.test.code.api.dto.CreateUserRequestDto;
import com.capgemini.test.code.api.dto.UserResponseDto;
import com.capgemini.test.code.entity.Room;
import com.capgemini.test.code.entity.User;
import com.capgemini.test.code.mapper.UserMapper;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private final UserMapper mapper = new UserMapper();

  @Test
  void toEntity_mapsAllFieldsAndRolToRole() {
    CreateUserRequestDto dto = new CreateUserRequestDto()
        .name("pablo")
        .email("email@email.com")
        .phone("677998899")
        .rol("admin")
        .dni("23454234W");

    User user = mapper.toEntity(dto);

    assertThat(user.getId()).isNull();
    assertThat(user.getName()).isEqualTo("pablo");
    assertThat(user.getEmail()).isEqualTo("email@email.com");
    assertThat(user.getPhone()).isEqualTo("677998899");
    assertThat(user.getRole()).isEqualTo("admin");
    assertThat(user.getDni()).isEqualTo("23454234W");
  }

  @Test
  void toDto_mapsAllFieldsAndRoomId() {
    Room room = new Room();
    room.setId(1L);
    User user = new User();
    user.setId(7L);
    user.setName("pablo");
    user.setEmail("email@email.com");
    user.setPhone("677998899");
    user.setRole("admin");
    user.setDni("23454234W");
    user.setRoom(room);

    UserResponseDto dto = mapper.toDto(user);

    assertThat(dto.getId()).isEqualTo(7L);
    assertThat(dto.getRol()).isEqualTo("admin");
    assertThat(dto.getRoomId()).isEqualTo(1L);
  }

  @Test
  void nullInput_returnsNull() {
    assertThat(mapper.toEntity(null)).isNull();
    assertThat(mapper.toDto(null)).isNull();
  }
}
