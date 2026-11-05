package com.capgemini.test.code.exception;

public class RoomNotFoundException extends RuntimeException {

  public RoomNotFoundException(Long id) {
    super("room not found " + id);
  }
}
