package com.nutricook.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** US1 end to end: targets over HTTP, profile completeness and validation, on PostgreSQL. */
class TargetsIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;

  private long userId;
  private String token;

  @BeforeEach
  void register() throws Exception {
    JsonNode node =
        json.readTree(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"tgt-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    userId = node.get("userId").asLong();
    token = "Bearer " + node.get("token").asText();
  }

  private ResultActions putProfile(String body) throws Exception {
    return mockMvc.perform(
        put("/api/users/" + userId + "/profile")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions getTargets() throws Exception {
    return mockMvc.perform(get("/api/users/" + userId + "/targets").header("Authorization", token));
  }

  private static String profile(
      String gender, int age, double kg, double cm, String activity, String goal) {
    LocalDate dob = LocalDate.now(ZoneOffset.UTC).minusYears(age);
    return """
        {"dateOfBirth":"%s","gender":"%s","weightKg":%s,"heightCm":%s,
         "activityLevel":"%s","healthGoal":"%s"}
        """
        .formatted(dob, gender, kg, cm, activity, goal);
  }

  @ParameterizedTest(name = "case {0}")
  @CsvSource({
    "A, MALE,   30, 80, 180, MODERATELY_ACTIVE, MAINTAIN_WEIGHT, 2760,  96,  92, 387, 39, false",
    "B, FEMALE, 25, 60, 165, LIGHTLY_ACTIVE,    LOSE_WEIGHT,     1350, 108,  45, 128, 19, false",
    "C, FEMALE, 60, 50, 155, SEDENTARY,         LOSE_WEIGHT,     1200,  90,  40, 120, 17, true",
    "D, MALE,   20, 70, 175, VERY_ACTIVE,       GAIN_MUSCLE,     3230, 140, 108, 425, 45, false"
  })
  void BR_NRG_ReferenceCasesOverHttp(
      String name,
      String gender,
      int age,
      double kg,
      double cm,
      String activity,
      String goal,
      int kcal,
      int protein,
      int fat,
      int carbs,
      int fiber,
      boolean floorApplied)
      throws Exception {
    putProfile(profile(gender, age, kg, cm, activity, goal)).andExpect(status().isOk());

    getTargets()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kcal").value(kcal))
        .andExpect(jsonPath("$.proteinG").value(protein))
        .andExpect(jsonPath("$.fatG").value(fat))
        .andExpect(jsonPath("$.carbsG").value(carbs))
        .andExpect(jsonPath("$.fiberG").value(fiber))
        .andExpect(jsonPath("$.floorApplied").value(floorApplied))
        .andExpect(jsonPath("$.slots.length()").value(5))
        .andExpect(jsonPath("$.slots[0].slot").value("BREAKFAST"))
        .andExpect(jsonPath("$.slots[0].kcal").value(Math.round(kcal * 0.25)));
  }

  @Test
  void BR_PRF_01_newProfileIs422WithEveryMissingField() throws Exception {
    getTargets()
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PROFILE_INCOMPLETE"))
        .andExpect(
            jsonPath(
                "$.missingFields",
                Matchers.contains(
                    "dateOfBirth",
                    "gender",
                    "weightKg",
                    "heightCm",
                    "activityLevel",
                    "healthGoal")));
  }

  @Test
  void BR_PRF_01_planGenerationRequiresACompleteProfile() throws Exception {
    // Previously this silently planned 2000 kcal for anyone
    putProfile("{\"weightKg\":80,\"heightCm\":180}").andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/users/" + userId + "/diet-plans/generate").header("Authorization", token))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PROFILE_INCOMPLETE"))
        .andExpect(jsonPath("$.missingFields", Matchers.hasItem("dateOfBirth")));
  }

  @Test
  void BR_PRF_02_minorIs422Ineligible() throws Exception {
    putProfile(profile("FEMALE", 16, 55, 160, "LIGHTLY_ACTIVE", "MAINTAIN_WEIGHT"))
        .andExpect(status().isOk());

    getTargets()
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PROFILE_INELIGIBLE"));
  }

  @Test
  void BR_PRF_08_profileEmbedsTargetsOrTheReasonTheyAreMissing() throws Exception {
    mockMvc
        .perform(get("/api/users/" + userId + "/profile").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targets").doesNotExist())
        .andExpect(jsonPath("$.targetsError.code").value("PROFILE_INCOMPLETE"));

    putProfile(profile("MALE", 30, 80, 180, "MODERATELY_ACTIVE", "MAINTAIN_WEIGHT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targets.kcal").value(2760))
        .andExpect(jsonPath("$.targetsError").doesNotExist());
  }

  @Test
  void BR_PRF_03_weightAndHeightRanges() throws Exception {
    putProfile("{\"weightKg\":29.9}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("weightKg"));
    putProfile("{\"heightCm\":230.5}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("heightCm"));
    putProfile("{\"weightKg\":30,\"heightCm\":230}").andExpect(status().isOk());
    putProfile("{\"weightKg\":300,\"heightCm\":120}").andExpect(status().isOk());
  }

  @Test
  void BR_PRF_03_dateOfBirthMustBeInThePast() throws Exception {
    putProfile("{\"dateOfBirth\":\"" + LocalDate.now(ZoneOffset.UTC).plusDays(2) + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("dateOfBirth"));
  }

  @Test
  void BR_PRF_05_omittedFieldsAreUnchanged() throws Exception {
    putProfile(profile("MALE", 30, 80, 180, "MODERATELY_ACTIVE", "MAINTAIN_WEIGHT"))
        .andExpect(status().isOk());

    putProfile("{\"firstName\":\"Sami\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Sami"))
        .andExpect(jsonPath("$.weightKg").value(80.0))
        .andExpect(jsonPath("$.healthGoal").value("MAINTAIN_WEIGHT"))
        .andExpect(jsonPath("$.targets.kcal").value(2760));
  }

  @Test
  void BR_PRF_06_profileChangeChangesFutureTargets() throws Exception {
    putProfile(profile("MALE", 30, 80, 180, "MODERATELY_ACTIVE", "MAINTAIN_WEIGHT"))
        .andExpect(status().isOk());
    putProfile("{\"healthGoal\":\"LOSE_WEIGHT\"}").andExpect(status().isOk());

    getTargets().andExpect(jsonPath("$.kcal").value(2260));
  }

  @Test
  void BR_TIM_01_timezoneIsValidatedAndStored() throws Exception {
    putProfile("{\"timezone\":\"Mars/Olympus_Mons\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.field").value("timezone"));

    putProfile("{\"timezone\":\"Africa/Tunis\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("Africa/Tunis"));
  }

  @Test
  void someoneElsesTargetsAre403() throws Exception {
    mockMvc
        .perform(
            get("/api/users/" + (userId + 100_000) + "/targets").header("Authorization", token))
        .andExpect(status().isForbidden());
  }
}
