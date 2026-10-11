package com.nutricook.domain;

import java.util.Map;

/**
 * A business rule rejected the operation. The message is written for end users and is returned
 * verbatim; {@code details} carries structured context (e.g. {@code missingFields}, {@code
 * constraint}) that is exposed as ProblemDetail properties.
 */
public class DomainException extends RuntimeException {

  private final ErrorCode code;
  private final Map<String, Object> details;

  public DomainException(ErrorCode code, String message) {
    this(code, message, Map.of());
  }

  public DomainException(ErrorCode code, String message, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.details = Map.copyOf(details);
  }

  public ErrorCode code() {
    return code;
  }

  public Map<String, Object> details() {
    return details;
  }
}
