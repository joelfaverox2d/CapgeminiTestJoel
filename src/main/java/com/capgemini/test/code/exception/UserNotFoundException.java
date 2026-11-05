package com.capgemini.test.code.exception;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(Long id) {
    super("user not found " + id);
  }
}
