package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** US3 end to end on PostgreSQL: lifecycle, concurrency, quota, async AI, snapshots. */
class PlanGenerationIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager em;

  private long userId;
  private String token;
  private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

  @BeforeEach
  void setUp() throws Exception {
    chatModel.reset();
    JsonNode node =
        json.readTree(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"gen-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    userId = node.get("userId").asLong();
    token = "Bearer " + node.get("token").asText();
    mockMvc
        .perform(
            put("/api/users/" + userId + "/profile")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"dateOfBirth":"%s","gender":"MALE","weightKg":80,"heightCm":180,
                     "activityLevel":"MODERATELY_ACTIVE","healthGoal":"MAINTAIN_WEIGHT","timezone":"UTC"}
                    """
                        .formatted(today.minusYears(30))))
        .andExpect(status().isOk());
  }

  @AfterEach
  void tearDown() {
    chatModel.reset();
  }

  private MvcResult generate(String query) throws Exception {
    return mockMvc
        .perform(
            post("/api/users/" + userId + "/diet-plans/generate" + query)
                .header("Authorization", token))
        .andReturn();
  }

  private JsonNode generateOk() throws Exception {
    MvcResult r = generate("");
    assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
    return json.readTree(r.getResponse().getContentAsString());
  }

  private JsonNode getPlan(long planId) throws Exception {
    return json.readTree(
        mockMvc
            .perform(
                get("/api/users/" + userId + "/diet-plans/" + planId)
                    .header("Authorization", token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private long activePlans() {
    return jdbc.queryForObject(
        "select count(*) from diet_plans where user_id = ? and status = 'ACTIVE'",
        Long.class,
        userId);
  }

  @Test
  void generatedPlanHasRealMealsAndHitsTheTarget() throws Exception {
    JsonNode plan = generateOk();

    assertThat(plan.get("meals")).hasSize(5);
    for (JsonNode meal : plan.get("meals")) {
      assertThat(meal.get("components").size()).as(meal.get("mealType").asText()).isBetween(2, 4);
    }
    // Reference profile A: 2760 kcal, within ±10 % at worst (±5 % with no warning)
    assertThat(plan.get("totalCalories").asInt()).isBetween(2484, 3036);
    assertThat(plan.get("aiStatus").asText()).isEqualTo("PENDING");
    assertThat(plan.get("generationSeq").asInt()).isEqualTo(1);
  }

  @Test
  void BR_LIF_02_regeneratingSupersedesThePreviousPlan() throws Exception {
    long first = generateOk().get("id").asLong();
    JsonNode second = generateOk();

    assertThat(second.get("generationSeq").asInt()).isEqualTo(2);
    assertThat(getPlan(first).get("status").asText()).isEqualTo("SUPERSEDED");
    assertThat(activePlans()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from diet_plans where user_id = ?", Long.class, userId))
        .as("superseded plans are kept, never deleted")
        .isEqualTo(2);
  }

  @Test
  void BR_LIF_02_concurrentGenerationsLeaveExactlyOneActivePlan() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> results = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      results.add(
          pool.submit(
              () -> {
                start.await();
                return generate("").getResponse().getStatus();
              }));
    }
    start.countDown();
    for (Future<Integer> r : results) assertThat(r.get()).isEqualTo(201);
    pool.shutdown();

    assertThat(activePlans()).isEqualTo(1);
  }

  @Test
  void BR_LIF_06_theEleventhGenerationOfTheDayIsRejected() throws Exception {
    for (int i = 0; i < 10; i++) generateOk();

    MvcResult eleventh = generate("");
    assertThat(eleventh.getResponse().getStatus()).isEqualTo(429);
    assertThat(json.readTree(eleventh.getResponse().getContentAsString()).get("code").asText())
        .isEqualTo("GENERATION_LIMIT");
  }

  @Test
  void BR_LIF_03_plansOnlyForTodayToSevenDaysAhead() throws Exception {
    for (LocalDate bad : List.of(today.minusDays(1), today.plusDays(8))) {
      MvcResult r = generate("?date=" + bad);
      assertThat(r.getResponse().getStatus()).isEqualTo(400);
      assertThat(json.readTree(r.getResponse().getContentAsString()).get("field").asText())
          .isEqualTo("date");
    }
    assertThat(generate("?date=" + today.plusDays(7)).getResponse().getStatus()).isEqualTo(201);
  }

  @Test
  void BR_LIF_01_anArchivedPlanCannotBeArchivedAgain() throws Exception {
    long planId = generateOk().get("id").asLong();
    String archive = "/api/users/" + userId + "/diet-plans/" + planId + "/archive";

    mockMvc
        .perform(patch(archive).header("Authorization", token))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(patch(archive).header("Authorization", token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PLAN_READ_ONLY"));
  }

  @Test
  void BR_LIF_07_aiTextArrivesAfterCommit() throws Exception {
    long planId = generateOk().get("id").asLong();

    JsonNode plan = awaitAiStatus(planId, Duration.ofSeconds(10));
    assertThat(plan.get("aiStatus").asText()).isEqualTo("READY");
    assertThat(plan.get("aiExplanation").asText()).isEqualTo("Stub AI reply.");
  }

  @Test
  void BR_LIF_07_aSlowAiNeverDelaysTheResponse() throws Exception {
    chatModel.slowByDefault(Duration.ofSeconds(5)); // test timeout is 2 s

    long started = System.nanoTime();
    JsonNode plan = generateOk();
    long elapsedMs = (System.nanoTime() - started) / 1_000_000;

    assertThat(elapsedMs).as("generation must not wait for the AI").isLessThan(2_000);
    assertThat(plan.get("aiStatus").asText()).isEqualTo("PENDING");
    JsonNode later = awaitAiStatus(plan.get("id").asLong(), Duration.ofSeconds(10));
    assertThat(later.get("aiStatus").asText()).isEqualTo("UNAVAILABLE");
    assertThat(later.get("totalCalories").asInt()).isEqualTo(plan.get("totalCalories").asInt());
  }

  private JsonNode awaitAiStatus(long planId, Duration max) throws Exception {
    long deadline = System.nanoTime() + max.toNanos();
    JsonNode plan = getPlan(planId);
    while (plan.get("aiStatus").asText().equals("PENDING") && System.nanoTime() < deadline) {
      Thread.sleep(200);
      plan = getPlan(planId);
    }
    return plan;
  }

  @Test
  @Transactional // rolled back: the catalog edit never leaks into other tests
  void BR_LIF_05_catalogEditsNeverRewriteHistoricPlans() throws Exception {
    JsonNode before = generateOk();
    long planId = before.get("id").asLong();

    jdbc.update(
        """
        update food_items set calories_per100g = calories_per100g * 2, protein_per100g = 0
        where id in (select mfi.food_item_id from meal_food_items mfi
                     join meals m on m.id = mfi.meal_id where m.diet_plan_id = ?)""",
        planId);
    em.flush();
    em.clear();

    JsonNode after = getPlan(planId);
    assertThat(after.get("totalCalories")).isEqualTo(before.get("totalCalories"));
    assertThat(after.get("totalProteinG")).isEqualTo(before.get("totalProteinG"));
    for (int i = 0; i < 5; i++) {
      assertThat(after.get("meals").get(i).get("components"))
          .isEqualTo(before.get("meals").get(i).get("components"));
    }
  }

  @Test
  void BR_PRF_06_profileChangesDoNotAlterAnExistingPlan() throws Exception {
    JsonNode before = generateOk();

    mockMvc
        .perform(
            put("/api/users/" + userId + "/profile")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"healthGoal\":\"GAIN_MUSCLE\",\"weightKg\":95}"))
        .andExpect(status().isOk());

    JsonNode after = getPlan(before.get("id").asLong());
    assertThat(after.get("totalCalories")).isEqualTo(before.get("totalCalories"));
    // Compare everything but the AI tips, which arrive asynchronously (BR-LIF-07)
    assertThat(withoutAiText(after.get("meals"))).isEqualTo(withoutAiText(before.get("meals")));
  }

  private static JsonNode withoutAiText(JsonNode meals) {
    JsonNode copy = meals.deepCopy();
    copy.forEach(m -> ((com.fasterxml.jackson.databind.node.ObjectNode) m).remove("aiSuggestion"));
    return copy;
  }
}
