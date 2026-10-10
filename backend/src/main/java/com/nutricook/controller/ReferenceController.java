package com.nutricook.controller;

import com.nutricook.entity.DietaryRestriction;
import com.nutricook.entity.HealthCondition;
import com.nutricook.service.ReferenceDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceController {

  private final ReferenceDataService referenceData;

  @GetMapping("/dietary-restrictions")
  public ResponseEntity<List<DietaryRestriction>> getAllRestrictions() {
    return ResponseEntity.ok(referenceData.restrictions());
  }

  @GetMapping("/health-conditions")
  public ResponseEntity<List<HealthCondition>> getAllConditions() {
    return ResponseEntity.ok(referenceData.conditions());
  }
}
