package com.nutricook.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
  private String reply;
  private DietPlanResponse updatedPlan;
}
