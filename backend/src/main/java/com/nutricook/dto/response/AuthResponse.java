package com.nutricook.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
  private Long userId;
  private String token;
  private String email;
  private String role;
}
