package com.nutricook.controller;

import com.nutricook.domain.model.FoodRole;
import com.nutricook.dto.request.FoodRequest;
import com.nutricook.dto.response.FoodResponse;
import com.nutricook.dto.response.PageResponse;
import com.nutricook.service.CatalogAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Curated catalog administration (FR-005). Only ADMIN may use it (BR-CAT-05). */
@RestController
@RequestMapping("/api/admin/foods")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminFoodController {

  private final CatalogAdminService catalog;

  /** Sorted by name. {@code q} matches part of the name, any case. */
  @GetMapping
  public PageResponse<FoodResponse> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) FoodRole role,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponse.of(catalog.search(q, role, active, page, size), FoodResponse::of);
  }

  @GetMapping("/{id}")
  public FoodResponse get(@PathVariable Long id) {
    return FoodResponse.of(catalog.get(id));
  }

  @PostMapping
  public ResponseEntity<FoodResponse> create(@Valid @RequestBody FoodRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(FoodResponse.of(catalog.create(req)));
  }

  @PutMapping("/{id}")
  public FoodResponse update(@PathVariable Long id, @Valid @RequestBody FoodRequest req) {
    return FoodResponse.of(catalog.update(id, req));
  }

  /** Soft delete (BR-CAT-04): plans that used the food keep it. */
  @DeleteMapping("/{id}")
  public FoodResponse deactivate(@PathVariable Long id) {
    return FoodResponse.of(catalog.deactivate(id));
  }

  @PostMapping("/{id}/activate")
  public FoodResponse activate(@PathVariable Long id) {
    return FoodResponse.of(catalog.activate(id));
  }
}
