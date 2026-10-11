package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.entity.UserAchievement;
import com.nutricook.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** BR-ACH-02/03/05/06/07 through the service and the API. */
class AchievementIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired AchievementService achievements;
  @Autowired EntityManagerFactory emf;

  private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
  private long userId;
  private String token;

  private record TestUser(long id, String bearer) {}

  @BeforeEach
  void setUp() throws Exception {
    TestUser user = newUser("LOSE_WEIGHT");
    userId = user.id();
    token = user.bearer();
  }

  /** Registers a user with a complete profile. */
  private TestUser newUser(String goal) throws Exception {
    JsonNode node =
        read(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"ach-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andReturn());
    long id = node.get("userId").asLong();
    String bearer = "Bearer " + node.get("token").asText();
    mockMvc
        .perform(
            put("/api/users/" + id + "/profile")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"dateOfBirth":"%s","gender":"FEMALE","weightKg":80,"heightCm":170,
                     "activityLevel":"LIGHTLY_ACTIVE","healthGoal":"%s","timezone":"UTC"}
                    """
                        .formatted(today.minusYears(35), goal)))
        .andExpect(status().isOk());
    return new TestUser(id, bearer);
  }

  private JsonNode read(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  /** Inserts history directly, without triggering an evaluation. */
  private void history(long user, int daysAgo, Integer waterMl, Double weightKg) {
    jdbc.update(
        "insert into progress_entries (user_id, date, water_ml, weight_kg, meals_completed)"
            + " values (?, ?, ?, ?, 0)",
        user,
        today.minusDays(daysAgo),
        waterMl,
        weightKg);
  }

  private JsonNode standing(String name) throws Exception {
    JsonNode all =
        read(
            mockMvc
                .perform(
                    get("/api/users/" + userId + "/achievements").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn());
    for (JsonNode a : all) if (a.get("name").asText().equals(name)) return a;
    throw new AssertionError(name);
  }

  private JsonNode logProgress(String body) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                post("/api/users/" + userId + "/progress")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andReturn();
    assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
    return read(r);
  }

  private int awardRows(long user) {
    return jdbc.queryForObject(
        "select count(*) from user_achievements where user_id = ?", Integer.class, user);
  }

  // ── catalog ────────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_05_catalogHasOnlyMeasurableTypes() throws Exception {
    JsonNode all =
        read(mockMvc.perform(get("/api/achievements").header("Authorization", token)).andReturn());
    List<String> types = new ArrayList<>();
    all.forEach(a -> types.add(a.get("type").asText()));
    assertThat(types)
        .doesNotContain("PROTEIN_GOAL", "LOGIN_STREAK")
        .contains("LOGGING_STREAK", "WEIGHT_MILESTONE");
  }

  // ── BR-ACH-02 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_02_regenerationsCountOnce() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/api/users/" + userId + "/diet-plans/generate").header("Authorization", token))
          .andExpect(status().isCreated());
    }
    JsonNode planner = standing("Planner");
    assertThat(planner.get("currentValue").asDouble()).isEqualTo(1.0);
    assertThat(planner.get("threshold").asInt()).isEqualTo(5);
    assertThat(planner.get("earned").asBoolean()).isFalse();
    assertThat(standing("First Steps").get("earned").asBoolean()).isTrue();
  }

  // ── BR-ACH-03 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_03_aDayCountsAgainstItsOwnTarget() throws Exception {
    int target =
        logProgress("{\"date\":\"%s\",\"waterMl\":100}".formatted(today))
            .get("progress")
            .get("targetCalories")
            .asInt();

    logProgress("{\"date\":\"%s\",\"caloriesConsumed\":%d}".formatted(today, target * 2));
    assertThat(standing("Calorie Champion").get("currentValue").asDouble()).isZero();

    logProgress("{\"date\":\"%s\",\"caloriesConsumed\":%d}".formatted(today, target + target / 20));
    assertThat(standing("Calorie Champion").get("currentValue").asDouble()).isEqualTo(1.0);
  }

  // ── BR-ACH-05 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_05_weightMilestoneFollowsTheGoal() throws Exception {
    history(userId, 20, null, 80.0);
    JsonNode result = logProgress("{\"date\":\"%s\",\"weightKg\":77.5}".formatted(today));

    List<String> awarded = new ArrayList<>();
    result.get("newAchievements").forEach(a -> awarded.add(a.get("name").asText()));
    assertThat(awarded).contains("On the Move");
    JsonNode transformation = standing("Transformation");
    assertThat(transformation.get("currentValue").asDouble()).isEqualTo(2.5);
    assertThat(transformation.get("earned").asBoolean()).isFalse();
  }

  // ── BR-ACH-06 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_06_concurrentAwardSingleRow() throws Exception {
    // 7 days in a row with 2 L of water: Week Warrior and Hydration Hero are both due
    for (int d = 0; d < 7; d++) history(userId, d, 2000, null);

    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<List<UserAchievement>>> results = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  return achievements.checkAndAward(userId);
                }));
      }
      start.countDown();
      List<String> awarded = new ArrayList<>();
      for (Future<List<UserAchievement>> f : results) {
        f.get().forEach(ua -> awarded.add(ua.getAchievement().getName()));
      }
      // each achievement was awarded by exactly one of the two calls, and neither failed
      assertThat(awarded).containsExactlyInAnyOrder("Week Warrior", "Hydration Hero");
    } finally {
      pool.shutdownNow();
    }
    assertThat(awardRows(userId)).isEqualTo(2);
  }

  @Test
  void BR_ACH_06_awardNeverRevoked() throws Exception {
    for (int d = 0; d < 7; d++) history(userId, d, 2000, null);
    assertThat(achievements.checkAndAward(userId)).hasSize(2);

    // the data that earned it changes later
    jdbc.update("update progress_entries set water_ml = 0 where user_id = ?", userId);
    assertThat(achievements.checkAndAward(userId)).isEmpty();

    JsonNode hero = standing("Hydration Hero");
    assertThat(hero.get("earned").asBoolean()).isTrue();
    assertThat(hero.get("earnedAt").isNull()).isFalse();
    assertThat(hero.get("currentValue").asDouble()).isZero();
    assertThat(awardRows(userId)).isEqualTo(2);
  }

  // ── BR-ACH-07 ──────────────────────────────────────────────────────────────

  @Test
  void BR_ACH_07_queryCountDoesNotGrowWithHistory() throws Exception {
    // every other day, nothing that earns an award: only the history length differs
    history(userId, 3, null, null);
    long other = newUser("LOSE_WEIGHT").id();
    for (int d = 1; d < 200; d += 2) history(other, d, null, null);

    long shortHistory = statementsFor(userId);
    long longHistory = statementsFor(other);

    assertThat(longHistory).isEqualTo(shortHistory);
    // profile + its 2 eager collections, history range, first weigh-in, plan days, awards, catalog
    assertThat(longHistory).isLessThanOrEqualTo(8);
  }

  private long statementsFor(long user) {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    achievements.checkAndAward(user);
    return stats.getPrepareStatementCount();
  }
}
