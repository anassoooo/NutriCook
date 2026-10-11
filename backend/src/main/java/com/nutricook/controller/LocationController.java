package com.nutricook.controller;

import com.nutricook.dto.response.LocationDTO;
import com.nutricook.service.LocationService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Nearby places from OpenStreetMap. {@code MAP_UNAVAILABLE} (503) when the map servers fail. */
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Validated
public class LocationController {

  private final LocationService locationService;

  @GetMapping("/restaurants")
  public ResponseEntity<List<LocationDTO>> restaurants(
      @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
      @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng) {
    return ResponseEntity.ok(locationService.getNearbyRestaurants(lat, lng));
  }

  @GetMapping("/stores")
  public ResponseEntity<List<LocationDTO>> stores(
      @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
      @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng) {
    return ResponseEntity.ok(locationService.getNearbyStores(lat, lng));
  }
}
