package com.nutricook.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorBody> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ErrorBody> handleDuplicateEmail(DuplicateEmailException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            e -> {
              String field = e instanceof FieldError fe ? fe.getField() : e.getObjectName();
              errors.put(field, e.getDefaultMessage());
            });
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorBody> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Invalid email or password."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorBody> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error("Unexpected error: " + ex.getMessage()));
  }

  private ErrorBody error(String message) {
    return new ErrorBody(message, LocalDateTime.now());
  }

  public record ErrorBody(String message, LocalDateTime timestamp) {}
}
