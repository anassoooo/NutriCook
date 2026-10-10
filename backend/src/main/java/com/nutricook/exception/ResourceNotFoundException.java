package com.nutricook.exception;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;

public class ResourceNotFoundException extends DomainException {
  public ResourceNotFoundException(String message) {
    super(ErrorCode.NOT_FOUND, message);
  }
}
