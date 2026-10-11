package com.nutricook.dto.response;

import com.nutricook.entity.UserAchievement;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AchievementResponse {
  private Long id;
  private String name;
  private String description;
  private String iconName;
  private String type;
  private Integer threshold;
  private LocalDateTime earnedAt;

  public static AchievementResponse of(UserAchievement ua) {
    return AchievementResponse.builder()
        .id(ua.getAchievement().getId())
        .name(ua.getAchievement().getName())
        .description(ua.getAchievement().getDescription())
        .iconName(ua.getAchievement().getIconName())
        .type(ua.getAchievement().getType().name())
        .threshold(ua.getAchievement().getThreshold())
        .earnedAt(ua.getEarnedAt())
        .build();
  }
}
