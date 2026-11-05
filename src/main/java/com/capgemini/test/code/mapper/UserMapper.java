package com.capgemini.test.code.mapper;

import com.capgemini.test.code.api.dto.CreateUserRequestDto;
import com.capgemini.test.code.api.dto.UserResponseDto;
import com.capgemini.test.code.entity.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {

  public User toEntity(CreateUserRequestDto dto) {
    if (dto == null) {
      return null;
    }
    User user = new User();
    user.setName(dto.getName());
    user.setEmail(dto.getEmail());
    user.setPhone(dto.getPhone());
    user.setRole(dto.getRol());
    user.setDni(dto.getDni());
    return user;
  }

  public UserResponseDto toDto(User user) {
    if (user == null) {
      return null;
    }
    return new UserResponseDto()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .phone(user.getPhone())
        .rol(user.getRole())
        .dni(user.getDni())
        .roomId(user.getRoom() != null ? user.getRoom().getId() : null);
  }
}
