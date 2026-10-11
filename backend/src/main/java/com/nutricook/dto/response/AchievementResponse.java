package com.nutricook.dto.response;

import com.nutricook.entity.Achievement;
import com.nutricook.entity.UserAchievement;
import com.nutricook.service.AchievementService;
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

  /** How far the user is, in the threshold's unit; the client never computes it. */
  private Double currentValue;

  private boolean earned;
  private LocalDateTime earnedAt;

  /** The catalog entry alone, without a user. */
  public static AchievementResponse of(Achievement a) {
    return base(a).build();
  }

  /** A newly awarded achievement. */
  public static AchievementResponse of(UserAchievement ua) {
    return base(ua.getAchievement()).earned(true).earnedAt(ua.getEarnedAt()).build();
  }

  public static AchievementResponse of(AchievementService.Standing s) {
    return base(s.achievement())
        .currentValue(s.currentValue())
        .earned(s.earned())
        .earnedAt(s.earnedAt())
        .build();
  }

  private static AchievementResponseBuilder base(Achievement a) {
    return AchievementResponse.builder()
        .id(a.getId())
        .name(a.getName())
        .description(a.getDescription())
        .iconName(a.getIconName())
        .type(a.getType().name())
        .threshold(a.getThreshold());
  }
}
