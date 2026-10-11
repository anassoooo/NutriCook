package com.nutricook.repository;

import com.nutricook.domain.model.AchievementType;
import com.nutricook.entity.Achievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
  List<Achievement> findByType(AchievementType type);
}
