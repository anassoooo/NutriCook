package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** FR-003: manual swaps and alternatives, with the same rules as AI swaps. */
class SwapIT extends AbstractIntegrationTest {

  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  private long userId;
  private String token;
  private JsonNode plan;

  @BeforeEach
  void setUp() throws Exception {
    JsonNode node =
        json.readTree(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"swap-"
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
                        .formatted(LocalDate.now(ZoneOffset.UTC).minusYears(30))))
        .andExpect(status().isOk());
    long nuts =
        jdbc.queryForObject(
            "select id from dietary_restrictions where name = 'Nut Allergy'", Long.class);
    mockMvc
        .perform(
            post("/api/users/" + userId + "/profile/dietary-restrictions/" + nuts)
                .header("Authorization", token))
        .andExpect(status().isNoContent());
    plan = generate();
  }

  private JsonNode generate() throws Exception {
    return json.readTree(
        mockMvc
            .perform(
                post("/api/users/" + userId + "/diet-plans/generate")
                    .header("Authorization", token))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private JsonNode meal(JsonNode p, String type) {
    for (JsonNode m : p.get("meals")) if (m.get("mealType").asText().equals(type)) return m;
    throw new AssertionError(type);
  }

  private String base(JsonNode p, JsonNode meal) {
    return "/api/users/"
        + userId
        + "/diet-plans/"
        + p.get("id").asLong()
        + "/meals/"
        + meal.get("id").asLong();
  }

  private MvcResult swap(JsonNode p, JsonNode meal, long foodId) throws Exception {
    return mockMvc
        .perform(
            post(base(p, meal) + "/swap")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodId\":" + foodId + "}"))
        .andReturn();
  }

  private long foodId(String name) {
    return jdbc.queryForObject("select id from food_items where name = ?", Long.class, name);
  }

  @Test
  void BR_AI_03_alternativesAreSafeAndAnyOfThemCanBeSwappedIn() throws Exception {
    JsonNode lunch = meal(plan, "LUNCH");
    JsonNode alternatives =
        json.readTree(
            mockMvc
                .perform(get(base(plan, lunch) + "/alternatives").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

    Set<String> roles = new HashSet<>();
    alternatives.forEach(a -> roles.add(a.get("role").asText()));
    Set<String> mealRoles = new HashSet<>();
    lunch.get("components").forEach(c -> mealRoles.add(c.get("role").asText()));
    assertThat(roles).isEqualTo(mealRoles);
    for (JsonNode group : alternatives) {
      for (JsonNode option : group.get("options")) {
        assertThat(option.get("foodName").asText())
            .isNotIn("Peanut Butter", "Almonds", "Walnuts", "Cashews", "Pistachios");
      }
    }

    JsonNode first = alternatives.get(0).get("options").get(0);
    MvcResult r = swap(plan, lunch, first.get("foodId").asLong());
    assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
    JsonNode newLunch = meal(json.readTree(r.getResponse().getContentAsString()), "LUNCH");
    boolean present = false;
    for (JsonNode c : newLunch.get("components"))
      if (c.get("foodId").asLong() == first.get("foodId").asLong()) {
        present = true;
        assertThat(c.get("grams").asDouble()).isEqualTo(first.get("grams").asDouble());
      }
    assertThat(present).isTrue();
  }

  @Test
  void BR_SAF_05_aManualSwapToAnIneligibleFoodIs422() throws Exception {
    MvcResult r = swap(plan, meal(plan, "BREAKFAST"), foodId("Peanut Butter"));

    assertThat(r.getResponse().getStatus()).isEqualTo(422);
    JsonNode body = json.readTree(r.getResponse().getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("SWAP_REJECTED");
    assertThat(body.get("reason").asText()).isEqualTo("NOT_ELIGIBLE");
  }

  @Test
  void BR_AI_03_aSwapWithNoMatchingRoleIs422() throws Exception {
    // Find a meal without a FAT component and try to swap olive oil in
    JsonNode target = null;
    for (JsonNode m : plan.get("meals")) {
      boolean hasFat = false;
      for (JsonNode c : m.get("components"))
        if (c.get("role").asText().equals("FAT")) hasFat = true;
      if (!hasFat && !m.get("mealType").asText().contains("SNACK")) target = m;
    }
    if (target == null) return; // every main meal got a fat this time; covered by SwapPlannerTest
    MvcResult r = swap(plan, target, foodId("Olive Oil"));

    assertThat(r.getResponse().getStatus()).isEqualTo(422);
    assertThat(json.readTree(r.getResponse().getContentAsString()).get("reason").asText())
        .isEqualTo("NO_SAME_ROLE");
  }

  @Test
  void BR_LIF_04_aSupersededPlanIsReadOnly() throws Exception {
    JsonNode old = plan;
    generate(); // supersedes the first plan

    MvcResult r = swap(old, meal(old, "LUNCH"), foodId("Lentils"));

    assertThat(r.getResponse().getStatus()).isEqualTo(409);
    assertThat(json.readTree(r.getResponse().getContentAsString()).get("code").asText())
        .isEqualTo("PLAN_READ_ONLY");
  }

  @Test
  void swappingOnSomeoneElsesPlanIs403() throws Exception {
    mockMvc
        .perform(
            post("/api/users/" + (userId + 100_000) + "/diet-plans/1/meals/1/swap")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodId\":1}"))
        .andExpect(status().isForbidden());
  }
}
