package com.nutricook.repository;

import com.nutricook.entity.UserAchievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
  List<UserAchievement> findByUserId(Long userId);

  boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
}
