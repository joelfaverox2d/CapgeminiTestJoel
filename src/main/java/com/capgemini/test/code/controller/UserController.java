package com.capgemini.test.code.controller;

import com.capgemini.test.code.api.UsersApi;
import com.capgemini.test.code.api.dto.ChangeRoomRequestDto;
import com.capgemini.test.code.api.dto.CreateUserRequestDto;
import com.capgemini.test.code.api.dto.IdResponseDto;
import com.capgemini.test.code.api.dto.UserResponseDto;
import com.capgemini.test.code.entity.User;
import com.capgemini.test.code.mapper.UserMapper;
import com.capgemini.test.code.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

  private final UserService userService;
  private final UserMapper userMapper;

  @Override
  public ResponseEntity<IdResponseDto> createUser(CreateUserRequestDto createUserRequestDto) {
    User user = userMapper.toEntity(createUserRequestDto);
    Long id = userService.createUser(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponseDto().id(id));
  }

  @Override
  public ResponseEntity<UserResponseDto> getUserById(Long id) {
    User user = userService.getUser(id);
    return ResponseEntity.ok(userMapper.toDto(user));
  }

  @Override
  public ResponseEntity<UserResponseDto> changeUserRoom(Long id, ChangeRoomRequestDto changeRoomRequestDto) {
    User user = userService.changeRoom(id, changeRoomRequestDto.getRoomId());
    return ResponseEntity.ok(userMapper.toDto(user));
  }
}
