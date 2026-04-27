package com.nutricook.service;

import com.nutricook.entity.Achievement;
import com.nutricook.entity.User;
import com.nutricook.entity.UserAchievement;
import com.nutricook.repository.AchievementRepository;
import com.nutricook.repository.DietPlanRepository;
import com.nutricook.repository.ProgressRepository;
import com.nutricook.repository.UserAchievementRepository;
import com.nutricook.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final ProgressRepository progressRepository;
  private final DietPlanRepository planRepository;
  private final UserRepository userRepository;

  public List<UserAchievement> getUserAchievements(Long userId) {
    return userAchievementRepository.findByUserId(userId);
  }

  @Transactional
  public List<UserAchievement> checkAndAward(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    List<UserAchievement> awarded = new ArrayList<>();

    for (Achievement achievement : achievementRepository.findAll()) {
      if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId()))
        continue;

      boolean earned =
          switch (achievement.getType()) {
            case LOGIN_STREAK -> evaluateStreak(userId) >= achievement.getThreshold();
            case PLANS_GENERATED -> planRepository.findByUserIdOrderByGeneratedAtDesc(userId).size()
                >= achievement.getThreshold();
            case CALORIE_GOAL -> countDaysWithCalorieGoal(userId) >= achievement.getThreshold();
            case HYDRATION -> countDaysWithHydration(userId) >= achievement.getThreshold();
            default -> false;
          };

      if (earned) {
        UserAchievement ua = UserAchievement.builder().user(user).achievement(achievement).build();
        awarded.add(userAchievementRepository.save(ua));
      }
    }
    return awarded;
  }

  private int evaluateStreak(Long userId) {
    LocalDate today = LocalDate.now();
    int streak = 0;
    for (int i = 0; i < 365; i++) {
      LocalDate day = today.minusDays(i);
      if (progressRepository.findByUserIdAndDate(userId, day).isPresent()) streak++;
      else break;
    }
    return streak;
  }

  private long countDaysWithCalorieGoal(Long userId) {
    return progressRepository
        .findByUserIdAndDateBetweenOrderByDateAsc(
            userId, LocalDate.now().minusDays(30), LocalDate.now())
        .stream()
        .filter(p -> p.getCaloriesConsumed() > 0)
        .count();
  }

  private long countDaysWithHydration(Long userId) {
    return progressRepository
        .findByUserIdAndDateBetweenOrderByDateAsc(
            userId, LocalDate.now().minusDays(30), LocalDate.now())
        .stream()
        .filter(p -> p.getWaterMl() >= 2000)
        .count();
  }
}
