package com.nutricook.repository;

import com.nutricook.entity.Achievement;
import com.nutricook.entity.enums.AchievementType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
  List<Achievement> findByType(AchievementType type);
}
