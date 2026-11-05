package com.capgemini.test.code.exception;


public class ValidationException extends RuntimeException {

  public ValidationException(String field) {
    super("error validation " + field);
  }
}
