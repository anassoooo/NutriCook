package com.nutricook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
  @NotBlank @Email private String email;

  @NotBlank
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;
}
