package com.nutricook.controller;

import com.nutricook.dto.response.LocationDTO;
import com.nutricook.service.LocationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;

  @GetMapping("/restaurants")
  public ResponseEntity<List<LocationDTO>> restaurants(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(required = false, defaultValue = "") List<String> restrictions) {
    return ResponseEntity.ok(locationService.getNearbyRestaurants(lat, lng, restrictions));
  }

  @GetMapping("/stores")
  public ResponseEntity<List<LocationDTO>> stores(
      @RequestParam double lat, @RequestParam double lng) {
    return ResponseEntity.ok(locationService.getNearbyStores(lat, lng));
  }
}
