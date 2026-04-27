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

  @NotNull
  @DecimalMin("20.0")
  @DecimalMax("500.0")
  private Double weightKg;

  @NotNull
  @DecimalMin("50.0")
  @DecimalMax("300.0")
  private Double heightCm;

  private String activityLevel;
  private String healthGoal;
}
