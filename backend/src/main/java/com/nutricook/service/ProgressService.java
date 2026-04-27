package com.nutricook.service;

import com.nutricook.dto.request.ProgressRequest;
import com.nutricook.entity.ProgressEntry;
import com.nutricook.entity.User;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.ProgressRepository;
import com.nutricook.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

  private final ProgressRepository progressRepository;
  private final UserRepository userRepository;

  @Transactional
  public ProgressEntry log(Long userId, ProgressRequest req) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    ProgressEntry entry =
        progressRepository
            .findByUserIdAndDate(userId, req.getDate())
            .orElse(ProgressEntry.builder().user(user).date(req.getDate()).build());

    if (req.getCaloriesConsumed() != null) entry.setCaloriesConsumed(req.getCaloriesConsumed());
    if (req.getWaterMl() != null) entry.setWaterMl(req.getWaterMl());
    if (req.getMealsCompleted() != null) entry.setMealsCompleted(req.getMealsCompleted());
    if (req.getExerciseMinutes() != null) entry.setExerciseMinutes(req.getExerciseMinutes());
    if (req.getWeightKg() != null) entry.setWeightKg(req.getWeightKg());
    if (req.getNotes() != null) entry.setNotes(req.getNotes());

    return progressRepository.save(entry);
  }

  public List<ProgressEntry> getRange(Long userId, LocalDate start, LocalDate end) {
    return progressRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end);
  }
}
