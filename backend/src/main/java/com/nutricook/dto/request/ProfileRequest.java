package com.nutricook.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ProfileRequest {
  private String firstName;
  private String lastName;
  @Past private LocalDate dateOfBirth;
  private String gender;

  // BR-PRF-03. All fields are optional: null means "leave unchanged" (BR-PRF-05).
  @DecimalMin(value = "30.0", message = "Weight must be between 30 and 300 kg")
  @DecimalMax(value = "300.0", message = "Weight must be between 30 and 300 kg")
  private Double weightKg;

  @DecimalMin(value = "120.0", message = "Height must be between 120 and 230 cm")
  @DecimalMax(value = "230.0", message = "Height must be between 120 and 230 cm")
  private Double heightCm;

  private String activityLevel;
  private String healthGoal;

  /** IANA zone id, e.g. "Africa/Tunis" (BR-TIM-01). */
  @Size(max = 64)
  private String timezone;
}
