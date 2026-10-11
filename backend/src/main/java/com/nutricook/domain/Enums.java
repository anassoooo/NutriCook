package com.nutricook.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/** Strict parsing of user-supplied enum values (BR-PRF-04). */
public final class Enums {

  private Enums() {}

  /**
   * Parses {@code value} case-insensitively.
   *
   * @throws DomainException {@code VALIDATION_FAILED} listing the allowed values when unknown
   */
  public static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
    try {
      return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Invalid value for " + field + ".",
          Map.of("field", field, "allowedValues", allowedValues(type)));
    }
  }

  public static <E extends Enum<E>> java.util.List<String> allowedValues(Class<E> type) {
    return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
  }
}
