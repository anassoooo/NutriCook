package com.nutricook.controller;

import com.nutricook.entity.DietaryRestriction;
import com.nutricook.entity.HealthCondition;
import com.nutricook.repository.DietaryRestrictionRepository;
import com.nutricook.repository.HealthConditionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceController {

  private final DietaryRestrictionRepository restrictionRepo;
  private final HealthConditionRepository conditionRepo;

  @GetMapping("/dietary-restrictions")
  public ResponseEntity<List<DietaryRestriction>> getAllRestrictions() {
    return ResponseEntity.ok(restrictionRepo.findAll());
  }

  @GetMapping("/health-conditions")
  public ResponseEntity<List<HealthCondition>> getAllConditions() {
    return ResponseEntity.ok(conditionRepo.findAll());
  }
}
