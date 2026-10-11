package com.nutricook.domain.model;

import java.util.Objects;

public record Warning(WarningCode code, String message) {
  public Warning {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
  }
}
