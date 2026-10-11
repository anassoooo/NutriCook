package com.nutricook.repository;

import com.nutricook.entity.UserAchievement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
  List<UserAchievement> findByUserId(Long userId);

  List<UserAchievement> findByUserIdAndAchievementIdIn(
      Long userId, Collection<Long> achievementIds);

  /**
   * Awards an achievement once (BR-ACH-06). A concurrent or repeated award is a no-op instead of a
   * unique-constraint error.
   *
   * @return 1 when this call awarded it, 0 when it was already awarded
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO user_achievements (user_id, achievement_id, earned_at)"
              + " VALUES (:userId, :achievementId, :earnedAt)"
              + " ON CONFLICT (user_id, achievement_id) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(Long userId, Long achievementId, LocalDateTime earnedAt);
}
