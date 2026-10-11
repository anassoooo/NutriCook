package com.nutricook.exception;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;

public class DuplicateEmailException extends DomainException {
  public DuplicateEmailException(String email) {
    super(ErrorCode.EMAIL_TAKEN, "Email already registered: " + email);
  }
}
