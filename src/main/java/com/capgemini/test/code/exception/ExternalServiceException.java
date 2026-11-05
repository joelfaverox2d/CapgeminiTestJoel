package com.capgemini.test.code.exception;


public class ExternalServiceException extends RuntimeException {

  public ExternalServiceException(String message) {
    super(message);
  }
}
