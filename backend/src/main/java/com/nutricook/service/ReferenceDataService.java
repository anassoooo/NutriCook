package com.nutricook.service;

import com.nutricook.entity.DietaryRestriction;
import com.nutricook.entity.HealthCondition;
import com.nutricook.repository.DietaryRestrictionRepository;
import com.nutricook.repository.HealthConditionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only reference data shown in onboarding and the profile page. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataService {

  private final DietaryRestrictionRepository restrictionRepository;
  private final HealthConditionRepository conditionRepository;

  public List<DietaryRestriction> restrictions() {
    return restrictionRepository.findAll();
  }

  public List<HealthCondition> conditions() {
    return conditionRepository.findAll();
  }
}
