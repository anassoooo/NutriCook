package com.nutricook.dto.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** One page of a list, with a stable JSON shape (Spring's {@code PageImpl} has none). */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

  public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
    return new PageResponse<>(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
