package com.nutricook.dto.response;

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
}
