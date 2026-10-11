package com.nutricook.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

/** Request ids, structured logs without user content, and admin-only metrics. */
@ExtendWith(OutputCaptureExtension.class)
class ObservabilityIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void tearDown() {
    chatModel.reset();
  }

  /** Registers a user with a complete profile; returns {userId, bearer}. */
  private String[] newUser() throws Exception {
    JsonNode reg =
        json.readTree(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"obs-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    String id = reg.get("userId").asText();
    String bearer = "Bearer " + reg.get("token").asText();
    mockMvc.perform(
        put("/api/users/" + id + "/profile")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"dateOfBirth":"%s","gender":"MALE","weightKg":80,"heightCm":180,
                 "activityLevel":"MODERATELY_ACTIVE","healthGoal":"MAINTAIN_WEIGHT","timezone":"UTC"}
                """
                    .formatted(LocalDate.now(ZoneOffset.UTC).minusYears(30))));
    return new String[] {id, bearer};
  }

  @Test
  void everyResponseCarriesARequestId() throws Exception {
    MockHttpServletResponse fresh =
        mockMvc.perform(get("/actuator/health")).andReturn().getResponse();
    assertThat(fresh.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");

    MockHttpServletResponse echoed =
        mockMvc
            .perform(get("/actuator/health").header("X-Request-Id", "support-ticket-42"))
            .andReturn()
            .getResponse();
    assertThat(echoed.getHeader("X-Request-Id")).isEqualTo("support-ticket-42");

    // a value that could forge log lines is replaced, not echoed
    MockHttpServletResponse unsafe =
        mockMvc
            .perform(get("/actuator/health").header("X-Request-Id", "x\nFAKE LOG LINE"))
            .andReturn()
            .getResponse();
    assertThat(unsafe.getHeader("X-Request-Id")).doesNotContain("FAKE");
  }

  @Test
  void errorBodiesNameTheirRequest() throws Exception {
    MockHttpServletResponse r =
        mockMvc
            .perform(get("/api/users/1/profile").header("X-Request-Id", "trace-me"))
            .andReturn()
            .getResponse();
    assertThat(r.getStatus()).isEqualTo(401);
    assertThat(json.readTree(r.getContentAsString()).get("requestId").asText())
        .isEqualTo("trace-me");
  }

  @Test
  void metricsAreForAdminsOnly() throws Exception {
    String[] user = newUser();
    assertThat(mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus())
        .isEqualTo(200);
    assertThat(
            mockMvc
                .perform(
                    get("/actuator/metrics/nutricook.plan.generation")
                        .header("Authorization", user[1]))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(403);

    jdbc.update("update users set role = 'ADMIN' where id = ?", Long.parseLong(user[0]));
    MockHttpServletResponse metrics =
        mockMvc
            .perform(
                get("/actuator/metrics/nutricook.plan.generation").header("Authorization", user[1]))
            .andReturn()
            .getResponse();
    assertThat(metrics.getStatus()).isEqualTo(200);
    // (Prometheus export is switched off in Spring Boot tests; it reads the same registry)
    assertThat(metrics.getContentAsString()).contains("\"name\":\"nutricook.plan.generation\"");
  }

  @Test
  void logsDescribeCallsWithoutUserContent(CapturedOutput output) throws Exception {
    String[] user = newUser();
    String planJson =
        mockMvc
            .perform(
                post("/api/users/" + user[0] + "/diet-plans/generate")
                    .header("Authorization", user[1])
                    .header("X-Request-Id", "gen-123"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long planId = json.readTree(planJson).get("id").asLong();
    assertThat(output.getOut()).containsPattern("\\[gen-123\\].*plan_generated planId=" + planId);

    // wait for the enrichment, so its scripted replies do not interfere
    for (int i = 0; i < 50; i++) {
      String status =
          jdbc.queryForObject(
              "select ai_status from diet_plans where id = ?", String.class, planId);
      if (!"PENDING".equals(status)) break;
      Thread.sleep(100);
    }
    chatModel.reply("{\"reply\":\"Sure.\",\"swap\":null}");
    String secret = "my-private-diary-" + UUID.randomUUID();
    mockMvc.perform(
        post("/api/users/" + user[0] + "/diet-plans/" + planId + "/chat")
            .header("Authorization", user[1])
            .header("X-Request-Id", "chat-456")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("message", secret))));

    assertThat(output.getOut())
        .containsPattern("\\[chat-456\\].*ai_call prompt=chat outcome=ok ms=\\d+")
        .doesNotContain(secret);
  }
}
