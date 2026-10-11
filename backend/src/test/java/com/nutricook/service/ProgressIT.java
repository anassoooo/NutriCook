package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.entity.enums.AiStatus;
import com.nutricook.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** BR-PRG-01..06 through the API: logging, meal check-off and the profile weight. */
class ProgressIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;
  @Autowired PlanCommandService commands;

  private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
  private long userId;
  private String token;

  @BeforeEach
  void setUp() throws Exception {
    JsonNode node =
        read(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"progress-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andReturn());
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

  // ── helpers ────────────────────────────────────────────────────────────────

  private JsonNode read(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  private MvcResult log(String body) throws Exception {
    return mockMvc
        .perform(
            post("/api/users/" + userId + "/progress")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn();
  }

  private JsonNode logOk(String body) throws Exception {
    MvcResult r = log(body);
    assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
    return read(r).get("progress");
  }

  private JsonNode generate(LocalDate date) throws Exception {
    return read(
        mockMvc
            .perform(
                post("/api/users/" + userId + "/diet-plans/generate?date=" + date)
                    .header("Authorization", token))
            .andExpect(status().isCreated())
            .andReturn());
  }

  private MvcResult complete(JsonNode plan, JsonNode meal, boolean completed) throws Exception {
    return mockMvc
        .perform(
            put("/api/users/"
                    + userId
                    + "/diet-plans/"
                    + plan.get("id").asLong()
                    + "/meals/"
                    + meal.get("id").asLong()
                    + "/completion")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":" + completed + "}"))
        .andReturn();
  }

  private JsonNode completeOk(JsonNode plan, JsonNode meal, boolean completed) throws Exception {
    MvcResult r = complete(plan, meal, completed);
    assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
    return read(r);
  }

  private JsonNode day(LocalDate date) throws Exception {
    JsonNode range =
        read(
            mockMvc
                .perform(
                    get("/api/users/" + userId + "/progress/weekly?start=" + date + "&end=" + date)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn());
    return range.isEmpty() ? null : range.get(0);
  }

  private double profileWeight() throws Exception {
    return read(mockMvc
            .perform(get("/api/users/" + userId + "/profile").header("Authorization", token))
            .andExpect(status().isOk())
            .andReturn())
        .get("weightKg")
        .asDouble();
  }

  private int targetKcal() throws Exception {
    return read(mockMvc
            .perform(get("/api/users/" + userId + "/targets").header("Authorization", token))
            .andExpect(status().isOk())
            .andReturn())
        .get("kcal")
        .asInt();
  }

  private static List<String> errorFields(JsonNode problem) {
    List<String> fields = new ArrayList<>();
    problem.get("errors").forEach(e -> fields.add(e.get("field").asText()));
    return fields;
  }

  // ── BR-PRG-01 ──────────────────────────────────────────────────────────────

  @Test
  void BR_PRG_01_loggingAgainMergesFields() throws Exception {
    logOk("{\"date\":\"%s\",\"waterMl\":1500,\"notes\":\"good day\"}".formatted(today));
    JsonNode entry = logOk("{\"date\":\"%s\",\"exerciseMinutes\":40}".formatted(today));

    assertThat(entry.get("waterMl").asInt()).isEqualTo(1500);
    assertThat(entry.get("exerciseMinutes").asInt()).isEqualTo(40);
    assertThat(entry.get("notes").asText()).isEqualTo("good day");
  }

  // ── BR-PRG-02 ──────────────────────────────────────────────────────────────

  @Test
  void BR_PRG_02_futureAndTooOldDatesAreRejected() throws Exception {
    for (LocalDate date : List.of(today.plusDays(1), today.minusDays(31))) {
      MvcResult r = log("{\"date\":\"%s\",\"waterMl\":500}".formatted(date));
      assertThat(r.getResponse().getStatus()).isEqualTo(400);
      JsonNode problem = read(r);
      assertThat(problem.get("code").asText()).isEqualTo("VALIDATION_FAILED");
      assertThat(problem.get("field").asText()).isEqualTo("date");
    }
    assertThat(log("{\"date\":\"%s\",\"waterMl\":500}".formatted(today.minusDays(30))))
        .extracting(r -> r.getResponse().getStatus())
        .isEqualTo(200);
  }

  @Test
  void BR_PRG_02_mealsOfAFuturePlanCannotBeCheckedOff() throws Exception {
    JsonNode tomorrow = generate(today.plusDays(1));
    MvcResult r = complete(tomorrow, tomorrow.get("meals").get(0), true);
    assertThat(r.getResponse().getStatus()).isEqualTo(400);
    assertThat(read(r).get("code").asText()).isEqualTo("VALIDATION_FAILED");
  }

  // ── BR-PRG-03 ──────────────────────────────────────────────────────────────

  @Test
  void BR_PRG_03_outOfRangeValuesAreAllReported() throws Exception {
    MvcResult r =
        log(
            "{\"date\":\"%s\",\"caloriesConsumed\":20000,\"waterMl\":-1,\"weightKg\":12}"
                .formatted(today));
    assertThat(r.getResponse().getStatus()).isEqualTo(400);
    JsonNode problem = read(r);
    assertThat(problem.get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(errorFields(problem)).containsExactly("caloriesConsumed", "waterMl", "weightKg");
    assertThat(day(today)).isNull(); // nothing was saved
  }

  // ── BR-PRG-04 ──────────────────────────────────────────────────────────────

  @Test
  void BR_PRG_04_latestWeightUpdatesProfileAndTargets() throws Exception {
    int before = targetKcal();

    logOk("{\"date\":\"%s\",\"weightKg\":72}".formatted(today));

    assertThat(profileWeight()).isEqualTo(72.0);
    assertThat(targetKcal()).isLessThan(before); // lighter body, lower BMR
  }

  @Test
  void BR_PRG_04_backfilledWeightDoesNotOverwrite() throws Exception {
    logOk("{\"date\":\"%s\",\"weightKg\":78}".formatted(today));
    logOk("{\"date\":\"%s\",\"weightKg\":81}".formatted(today.minusDays(5)));

    assertThat(profileWeight()).isEqualTo(78.0);
    assertThat(day(today.minusDays(5)).get("weightKg").asDouble()).isEqualTo(81.0);
  }

  // ── BR-PRG-05 ──────────────────────────────────────────────────────────────

  @Test
  void BR_PRG_05_checkingMealsOffDerivesTheDay() throws Exception {
    JsonNode plan = generate(today);
    JsonNode breakfast = plan.get("meals").get(0);
    JsonNode lunch = plan.get("meals").get(2);

    completeOk(plan, breakfast, true);
    JsonNode result = completeOk(plan, lunch, true);

    JsonNode progress = result.get("progress");
    assertThat(progress.get("mealsCompleted").asInt()).isEqualTo(2);
    assertThat(progress.get("caloriesConsumed").asInt())
        .isEqualTo(breakfast.get("calories").asInt() + lunch.get("calories").asInt());
    assertThat(progress.get("caloriesOverridden").asBoolean()).isFalse();
    assertThat(result.get("plan").get("meals").get(0).get("completed").asBoolean()).isTrue();

    progress = completeOk(plan, breakfast, false).get("progress");
    assertThat(progress.get("mealsCompleted").asInt()).isEqualTo(1);
    assertThat(progress.get("caloriesConsumed").asInt()).isEqualTo(lunch.get("calories").asInt());
  }

  @Test
  void BR_PRG_05_typedCaloriesOverrideUntilReset() throws Exception {
    JsonNode plan = generate(today);
    JsonNode breakfast = plan.get("meals").get(0);
    completeOk(plan, breakfast, true);

    JsonNode entry = logOk("{\"date\":\"%s\",\"caloriesConsumed\":2600}".formatted(today));
    assertThat(entry.get("caloriesOverridden").asBoolean()).isTrue();

    JsonNode progress = completeOk(plan, plan.get("meals").get(1), true).get("progress");
    assertThat(progress.get("caloriesConsumed").asInt()).isEqualTo(2600);
    assertThat(progress.get("mealsCompleted").asInt()).isEqualTo(2);

    entry = logOk("{\"date\":\"%s\",\"resetCalories\":true}".formatted(today));
    assertThat(entry.get("caloriesOverridden").asBoolean()).isFalse();
    assertThat(entry.get("caloriesConsumed").asInt())
        .isEqualTo(
            breakfast.get("calories").asInt() + plan.get("meals").get(1).get("calories").asInt());
  }

  @Test
  void BR_PRG_05_mealsCompletedCannotBeTyped() throws Exception {
    JsonNode entry = logOk("{\"date\":\"%s\",\"mealsCompleted\":5}".formatted(today));
    assertThat(entry.get("mealsCompleted").asInt()).isZero();
  }

  @Test
  void BR_PRG_05_aCheckedMealCannotBeSwapped() throws Exception {
    JsonNode plan = generate(today);
    JsonNode lunch = plan.get("meals").get(2);
    completeOk(plan, lunch, true);

    long otherFood = lunch.get("components").get(0).get("foodId").asLong() + 1;
    MvcResult r =
        mockMvc
            .perform(
                post("/api/users/"
                        + userId
                        + "/diet-plans/"
                        + plan.get("id").asLong()
                        + "/meals/"
                        + lunch.get("id").asLong()
                        + "/swap")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"foodId\":" + otherFood + "}"))
            .andReturn();
    assertThat(r.getResponse().getStatus()).isEqualTo(422);
    assertThat(read(r).get("reason").asText()).isEqualTo("MEAL_COMPLETED");
  }

  @Test
  void BR_PRG_05_regeneratingTheDayFollowsTheNewPlan() throws Exception {
    JsonNode plan = generate(today);
    completeOk(plan, plan.get("meals").get(0), true);

    JsonNode regenerated = generate(today);

    JsonNode entry = day(today);
    assertThat(entry.get("mealsCompleted").asInt()).isZero();
    assertThat(entry.get("caloriesConsumed").asInt()).isZero();
    // the old plan is no longer the day's plan: it cannot be checked off
    MvcResult r = complete(plan, plan.get("meals").get(1), true);
    assertThat(read(r).get("code").asText()).isEqualTo("PLAN_READ_ONLY");
    assertThat(regenerated.get("meals").get(0).get("completed").asBoolean()).isFalse();
  }

  @Test
  void BR_PRG_05_archivingThePlanClearsDerivedCalories() throws Exception {
    JsonNode plan = generate(today);
    completeOk(plan, plan.get("meals").get(0), true);

    mockMvc
        .perform(
            patch("/api/users/" + userId + "/diet-plans/" + plan.get("id").asLong() + "/archive")
                .header("Authorization", token))
        .andExpect(status().isNoContent());

    JsonNode entry = day(today);
    assertThat(entry.get("mealsCompleted").asInt()).isZero();
    assertThat(entry.get("caloriesConsumed").isNull()).isTrue(); // no plan: unknown, not zero
  }

  @Test
  void BR_PRG_05_aiTextArrivingLateKeepsTheCheckOff() throws Exception {
    JsonNode plan = generate(today);
    JsonNode breakfast = plan.get("meals").get(0);
    completeOk(plan, breakfast, true);

    // what the asynchronous enrichment does when the AI answers after the check-off
    commands.saveAiText(
        plan.get("id").asLong(),
        "late explanation",
        Map.of(breakfast.get("id").asLong(), "late tip"),
        AiStatus.READY);

    JsonNode reloaded =
        read(
            mockMvc
                .perform(
                    get("/api/users/" + userId + "/diet-plans/" + plan.get("id").asLong())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode meal = reloaded.get("meals").get(0);
    assertThat(meal.get("completed").asBoolean()).isTrue();
    assertThat(meal.get("aiSuggestion").asText()).isEqualTo("late tip");
    assertThat(reloaded.get("aiExplanation").asText()).isEqualTo("late explanation");
  }

  // ── BR-PRG-06 ──────────────────────────────────────────────────────────────

  @Test
  void BR_PRG_06_targetSnapshotFromPlanElseProfile() throws Exception {
    int profileTarget = targetKcal();
    JsonNode yesterday = logOk("{\"date\":\"%s\",\"waterMl\":800}".formatted(today.minusDays(1)));
    assertThat(yesterday.get("targetCalories").asInt()).isEqualTo(profileTarget);

    JsonNode plan = generate(today);
    JsonNode entry = completeOk(plan, plan.get("meals").get(0), true).get("progress");
    assertThat(entry.get("targetCalories").asInt()).isEqualTo(profileTarget);

    // a later profile change does not move a past day's target
    logOk("{\"date\":\"%s\",\"weightKg\":70}".formatted(today));
    assertThat(targetKcal()).isNotEqualTo(profileTarget);
    logOk("{\"date\":\"%s\",\"exerciseMinutes\":20}".formatted(today.minusDays(1)));
    assertThat(day(today.minusDays(1)).get("targetCalories").asInt()).isEqualTo(profileTarget);
  }

  @Test
  void rangeMustBeOrderedAndBounded() throws Exception {
    mockMvc
        .perform(
            get("/api/users/"
                    + userId
                    + "/progress/weekly?start="
                    + today
                    + "&end="
                    + today.minusDays(1))
                .header("Authorization", token))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            get("/api/users/"
                    + userId
                    + "/progress/weekly?start="
                    + today.minusYears(2)
                    + "&end="
                    + today)
                .header("Authorization", token))
        .andExpect(status().isBadRequest());
  }
}
