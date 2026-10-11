package com.nutricook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
  /** BR-AI-06: 1-500 characters. */
  @NotBlank
  @Size(max = 500, message = "Messages can be at most 500 characters")
  private String message;
}
