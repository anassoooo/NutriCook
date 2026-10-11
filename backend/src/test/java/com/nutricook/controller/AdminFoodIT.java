package com.nutricook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** BR-CAT-01/02/04/05 through the admin API. */
class AdminFoodIT extends AbstractIntegrationTest {

  private static final String FOODS = "/api/admin/foods";

  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  private String admin;
  private String user;
  private long userId;

  @BeforeEach
  void setUp() throws Exception {
    // there is no default admin outside local development: promote a fresh account
    JsonNode adminReg =
        read(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"admin-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andReturn());
    jdbc.update("update users set role = 'ADMIN' where id = ?", adminReg.get("userId").asLong());
    admin = "Bearer " + adminReg.get("token").asText(); // roles are read per request
    JsonNode reg =
        read(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\"cat-"
                                + UUID.randomUUID()
                                + "@t.dev\",\"password\":\"Password123!\"}"))
                .andReturn());
    userId = reg.get("userId").asLong();
    user = "Bearer " + reg.get("token").asText();
    mockMvc
        .perform(
            put("/api/users/" + userId + "/profile")
                .header("Authorization", user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"dateOfBirth":"%s","gender":"MALE","weightKg":80,"heightCm":180,
                     "activityLevel":"MODERATELY_ACTIVE","healthGoal":"MAINTAIN_WEIGHT","timezone":"UTC"}
                    """
                        .formatted(LocalDate.now(ZoneOffset.UTC).minusYears(30))))
        .andExpect(status().isOk());
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private JsonNode read(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  private MvcResult call(MockHttpServletRequestBuilder req, String token) throws Exception {
    return mockMvc.perform(req.header("Authorization", token)).andReturn();
  }

  private MvcResult send(MockHttpServletRequestBuilder req, String token, String body)
      throws Exception {
    return call(req.contentType(MediaType.APPLICATION_JSON).content(body), token);
  }

  /** Seitan: values from USDA, consistent with BR-CAT-02. Placeholders: name, kcal, flags. */
  private static String food(String name, double kcal, String allergens, String flags) {
    return """
        {"name":"%s","category":"plant protein","role":"PROTEIN","state":"RAW",
         "caloriesPer100g":%s,"proteinPer100g":75,"carbsPer100g":14,"fatPer100g":1.9,
         "sugarPer100g":0,"fiberPer100g":0.6,"sodiumMgPer100g":29,
         "portionMinG":50,"portionMaxG":200,"portionStepG":5,"defaultPortionG":100,
         "allergens":[%s],"dietFlags":[%s],"suitableSlots":["LUNCH","DINNER"],
         "meat":false,"source":"USDA FoodData Central"}
        """
        .formatted(name, kcal, allergens, flags);
  }

  private static String seitan(String name) {
    return food(name, 370, "\"GLUTEN\"", "\"VEGAN\",\"VEGETARIAN\"");
  }

  private static String uniqueName() {
    return "Seitan " + UUID.randomUUID().toString().substring(0, 8);
  }

  private static List<String> errorFields(JsonNode problem) {
    List<String> fields = new ArrayList<>();
    problem.get("errors").forEach(e -> fields.add(e.get("field").asText()));
    return fields;
  }

  private long foodId(String name) {
    return jdbc.queryForObject("select id from food_items where name = ?", Long.class, name);
  }

  private JsonNode generate() throws Exception {
    MvcResult r = call(post("/api/users/" + userId + "/diet-plans/generate"), user);
    assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
    return read(r);
  }

  // ── BR-CAT-05 ──────────────────────────────────────────────────────────────

  @Test
  void BR_CAT_05_nonAdminForbidden() throws Exception {
    for (MockHttpServletRequestBuilder req :
        List.of(get(FOODS), post(FOODS), delete(FOODS + "/1"), post(FOODS + "/1/activate"))) {
      MvcResult r = send(req, user, seitan(uniqueName()));
      assertThat(r.getResponse().getStatus()).isEqualTo(403);
      assertThat(read(r).get("code").asText()).isEqualTo("FORBIDDEN");
    }
    assertThat(mockMvc.perform(get(FOODS)).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void BR_CAT_05_seededFoodsAreReadOnlyHere() throws Exception {
    long chicken = foodId("Chicken Breast");
    MvcResult r = send(put(FOODS + "/" + chicken), admin, seitan("Chicken Breast"));
    assertThat(r.getResponse().getStatus()).isEqualTo(409);
    assertThat(read(r).get("code").asText()).isEqualTo("FOOD_READ_ONLY");
    assertThat(
            jdbc.queryForObject(
                "select calories_per100g from food_items where id = ?", Double.class, chicken))
        .isEqualTo(165.0);
  }

  // ── BR-CAT-01 ──────────────────────────────────────────────────────────────

  @Test
  void BR_CAT_01_adminCreatesAndEditsItsOwnFood() throws Exception {
    String name = uniqueName();
    MvcResult created = send(post(FOODS), admin, seitan(name));
    assertThat(created.getResponse().getStatus())
        .as(created.getResponse().getContentAsString())
        .isEqualTo(201);
    JsonNode food = read(created);
    assertThat(food.get("origin").asText()).isEqualTo("ADMIN");
    assertThat(food.get("active").asBoolean()).isTrue();

    MvcResult updated =
        send(put(FOODS + "/" + food.get("id").asLong()), admin, seitan(name + " (firm)"));
    assertThat(updated.getResponse().getStatus()).isEqualTo(200);
    assertThat(read(updated).get("name").asText()).isEqualTo(name + " (firm)");
  }

  @Test
  void BR_CAT_01_namesAreUniqueIgnoringCase() throws Exception {
    MvcResult r = send(post(FOODS), admin, seitan("chicken BREAST"));
    assertThat(r.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(read(r))).containsExactly("name");
  }

  @Test
  void BR_CAT_01_searchFiltersAndPages() throws Exception {
    JsonNode page = read(call(get(FOODS + "?q=CHICK&role=PROTEIN&active=true&size=2"), admin));
    assertThat(page.get("size").asInt()).isEqualTo(2);
    assertThat(page.get("totalItems").asLong()).isGreaterThan(2);
    for (JsonNode f : page.get("items")) {
      assertThat(f.get("name").asText().toLowerCase()).contains("chick");
      assertThat(f.get("role").asText()).isEqualTo("PROTEIN");
    }
    MvcResult tooBig = call(get(FOODS + "?size=1000"), admin);
    assertThat(tooBig.getResponse().getStatus()).isEqualTo(400);
  }

  // ── BR-CAT-02 ──────────────────────────────────────────────────────────────

  @Test
  void BR_CAT_02_atwaterViolationRejected() throws Exception {
    String name = uniqueName();
    MvcResult r =
        send(post(FOODS), admin, food(name, 900, "\"GLUTEN\"", "\"VEGAN\",\"VEGETARIAN\""));
    assertThat(r.getResponse().getStatus()).isEqualTo(400);
    JsonNode problem = read(r);
    assertThat(problem.get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(errorFields(problem)).containsExactly("nutrients");
    assertThat(problem.get("errors").get(0).get("message").asText()).contains("energy 900");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from food_items where name = ?", Integer.class, name))
        .isZero();
  }

  @Test
  void BR_CAT_02_contradictoryDietLabelsRejected() throws Exception {
    MvcResult r =
        send(post(FOODS), admin, food(uniqueName(), 370, "\"MILK\"", "\"VEGAN\",\"VEGETARIAN\""));
    assertThat(r.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(read(r))).containsExactly("dietFlags");
  }

  @Test
  void BR_CAT_02_reactivationRequiresAPassingFood() throws Exception {
    String name = "Legacy " + UUID.randomUUID();
    jdbc.update(
        "insert into food_items (name, role, state, calories_per100g, protein_per100g,"
            + " carbs_per100g, fat_per100g, portion_min_g, portion_max_g, portion_step_g,"
            + " default_portion_g, active, allergens_known, origin)"
            + " values (?, 'CARB', 'COOKED', 130, 2.7, 28, 0.3, 50, 300, 25, 150, false, false,"
            + " 'SEED')",
        name);
    MvcResult r = call(post(FOODS + "/" + foodId(name) + "/activate"), admin);
    assertThat(r.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(read(r))).contains("suitableSlots", "source", "allergens");
  }

  // ── BR-CAT-04 ──────────────────────────────────────────────────────────────

  @Test
  void BR_CAT_04_softDeleteKeepsHistory() throws Exception {
    JsonNode plan = generate();
    JsonNode component = plan.get("meals").get(0).get("components").get(0);
    long food = component.get("foodId").asLong();
    try {
      MvcResult r = call(delete(FOODS + "/" + food), admin);
      assertThat(read(r).get("active").asBoolean()).isFalse();

      JsonNode reloaded =
          read(call(get("/api/users/" + userId + "/diet-plans/" + plan.get("id").asLong()), user));
      JsonNode same = reloaded.get("meals").get(0).get("components").get(0);
      assertThat(same.get("foodName").asText()).isEqualTo(component.get("foodName").asText());
      assertThat(same.get("kcal").asDouble()).isEqualTo(component.get("kcal").asDouble());
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from food_items where id = ?", Integer.class, food))
          .isOne();
    } finally {
      call(post(FOODS + "/" + food + "/activate"), admin); // shared database
    }
  }

  @Test
  void BR_CAT_04_inactiveExcludedFromNewPlans() throws Exception {
    JsonNode plan = generate();
    JsonNode lunch = plan.get("meals").get(2);
    long food = lunch.get("components").get(0).get("foodId").asLong();
    try {
      call(delete(FOODS + "/" + food), admin);

      JsonNode regenerated = generate();
      for (JsonNode meal : regenerated.get("meals")) {
        for (JsonNode c : meal.get("components")) {
          assertThat(c.get("foodId").asLong()).isNotEqualTo(food);
        }
      }
      JsonNode alternatives =
          read(
              call(
                  get(
                      "/api/users/"
                          + userId
                          + "/diet-plans/"
                          + regenerated.get("id").asLong()
                          + "/meals/"
                          + regenerated.get("meals").get(2).get("id").asLong()
                          + "/alternatives"),
                  user));
      for (JsonNode group : alternatives) {
        for (JsonNode option : group.get("options")) {
          assertThat(option.get("foodId").asLong()).isNotEqualTo(food);
        }
      }
    } finally {
      call(post(FOODS + "/" + food + "/activate"), admin);
    }
  }
}
