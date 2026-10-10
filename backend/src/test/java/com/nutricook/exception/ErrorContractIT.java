package com.nutricook.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.support.AbstractIntegrationTest;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** End-to-end error contract: real requests produce ProblemDetail bodies with stable codes. */
class ErrorContractIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;

  private long userId;
  private String token;

  @BeforeEach
  void registerUser() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"err-"
                            + UUID.randomUUID()
                            + "@t.dev\",\"password\":\"Password123!\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode node = json.readTree(body);
    userId = node.get("userId").asLong();
    token = "Bearer " + node.get("token").asText();
  }

  @Test
  void missingTokenIs401Unauthenticated() throws Exception {
    mockMvc
        .perform(get("/api/users/" + userId + "/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void garbageTokenIs401Unauthenticated() throws Exception {
    mockMvc
        .perform(get("/api/users/" + userId + "/profile").header("Authorization", "Bearer x.y.z"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void otherUsersResourceIs403Forbidden() throws Exception {
    mockMvc
        .perform(
            get("/api/users/" + (userId + 100_000) + "/profile").header("Authorization", token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void unknownPlanIs404NotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/users/" + userId + "/diet-plans/999999999").header("Authorization", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void unknownRouteIs404NotFound() throws Exception {
    mockMvc
        .perform(get("/api/does-not-exist").header("Authorization", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void malformedJsonIs400ValidationFailed() throws Exception {
    mockMvc
        .perform(
            put("/api/users/" + userId + "/profile")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void BR_PRF_04_unknownEnumIs400WithAllowedValues() throws Exception {
    mockMvc
        .perform(
            put("/api/users/" + userId + "/profile")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weightKg\":70,\"heightCm\":175,\"activityLevel\":\"COUCH_POTATO\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.field").value("activityLevel"))
        .andExpect(jsonPath("$.allowedValues", Matchers.hasItem("SEDENTARY")));
  }

  @Test
  void responsesNeverContainStackTracesOrExceptionNames() throws Exception {
    mockMvc
        .perform(get("/api/users/" + userId + "/diet-plans/abc").header("Authorization", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(content().string(Matchers.not(Matchers.containsString("Exception"))));
  }
}
