package com.capgemini.test.code.exception;

import com.capgemini.test.code.api.dto.ErrorResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponseDto> handleValidation(ValidationException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException ex) {
    return build(HttpStatus.CONFLICT, "error validation duplicated data");
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponseDto> handleExternalService(ExternalServiceException ex) {
    return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleNotFound(UserNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleRoomNotFound(RoomNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ErrorResponseDto().code(status.value()).message(message));
  }
}
