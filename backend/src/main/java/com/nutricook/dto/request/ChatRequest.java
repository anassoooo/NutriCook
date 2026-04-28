package com.nutricook.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
  @NotBlank private String message;
}
