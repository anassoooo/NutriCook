package com.nutricook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.UserProfile;
import com.nutricook.repository.FoodItemRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAiService {

  private final ChatClient.Builder chatClientBuilder;
  private final FoodItemRepository foodRepository;
  private final ObjectMapper mapper = new ObjectMapper();

  public record ChatResult(String reply, String swapMealType, String swapFoodName) {}

  public String getMealSuggestion(Meal meal, UserProfile profile) {
    try {
      String template = loadPrompt("prompts/meal-suggestion.st");
      String prompt =
          template
              .replace("{mealName}", meal.getName())
              .replace("{mealType}", meal.getMealType().name())
              .replace("{calories}", String.valueOf(meal.getCalories()))
              .replace("{protein}", String.valueOf(meal.getProteinG()))
              .replace(
                  "{goal}",
                  profile.getHealthGoal() != null
                      ? profile.getHealthGoal().name()
                      : "IMPROVE_HEALTH")
              .replace(
                  "{restrictions}",
                  profile.getDietaryRestrictions().stream()
                      .map(r -> r.getName())
                      .collect(Collectors.joining(", ", "", "")));
      if (prompt.isBlank()) return null;
      return chatClientBuilder.build().prompt(prompt).call().content();
    } catch (Exception e) {
      log.warn("Groq meal suggestion failed: {}", e.getMessage());
      return null;
    }
  }

  public String getPlanExplanation(DietPlan plan, UserProfile profile) {
    try {
      String template = loadPrompt("prompts/plan-explanation.st");
      String prompt =
          template
              .replace("{totalCalories}", String.valueOf(plan.getTotalCalories()))
              .replace("{totalProtein}", String.valueOf(plan.getTotalProteinG()))
              .replace(
                  "{goal}",
                  profile.getHealthGoal() != null
                      ? profile.getHealthGoal().name()
                      : "IMPROVE_HEALTH")
              .replace(
                  "{conditions}",
                  profile.getHealthConditions().stream()
                      .map(c -> c.getName())
                      .collect(Collectors.joining(", ", "", "")));
      if (prompt.isBlank()) return null;
      return chatClientBuilder.build().prompt(prompt).call().content();
    } catch (Exception e) {
      log.warn("Groq plan explanation failed: {}", e.getMessage());
      return null;
    }
  }

  public ChatResult chat(DietPlan plan, UserProfile profile, String userMessage, String progressContext) {
    String planSummary = buildPlanSummary(plan);
    String goal =
        profile.getHealthGoal() != null ? profile.getHealthGoal().name() : "IMPROVE_HEALTH";
    String restrictions =
        profile.getDietaryRestrictions().stream()
            .map(r -> r.getName())
            .collect(Collectors.joining(", "));
    if (restrictions.isBlank()) restrictions = "none";

    String foodList = foodRepository.findAll().stream()
        .map(f -> f.getName())
        .collect(Collectors.joining(", "));

    String prompt =
        """
        You are a friendly dietitian AI in the NutriCook app. The user's meal plan for today:

        %s
        Goal: %s | Dietary restrictions: %s
        %s

        User says: "%s"

        Reply ONLY with a valid JSON object (no markdown, no code blocks):
        - If the user wants to swap/replace/change a meal: {"reply":"<your friendly response>","swap":{"mealType":"<TYPE>","foodName":"<FOOD>"}}
        - Otherwise: {"reply":"<your friendly response>"}

        Valid mealType values: BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER
        Available foods: %s

        Keep the reply under 3 sentences. Be warm and practical.
        """
            .formatted(planSummary, goal, restrictions, progressContext, userMessage, foodList);

    try {
      String raw = chatClientBuilder.build().prompt(prompt).call().content();
      return parseChatResult(raw);
    } catch (Exception e) {
      log.warn("Groq chat failed: {}", e.getMessage());
      return new ChatResult(
          "Sorry, I couldn't process that right now. Please try again.", null, null);
    }
  }

  private String buildPlanSummary(DietPlan plan) {
    StringBuilder sb = new StringBuilder();
    for (Meal meal : plan.getMeals()) {
      sb.append(
          String.format(
              "- %s: %s (%d kcal, %.0fg protein, %.0fg carbs, %.0fg fat)%n",
              meal.getMealType(),
              meal.getName(),
              meal.getCalories() != null ? meal.getCalories() : 0,
              meal.getProteinG() != null ? meal.getProteinG() : 0,
              meal.getCarbsG() != null ? meal.getCarbsG() : 0,
              meal.getFatG() != null ? meal.getFatG() : 0));
    }
    return sb.toString();
  }

  private ChatResult parseChatResult(String raw) {
    try {
      int start = raw.indexOf('{');
      int end = raw.lastIndexOf('}');
      if (start == -1 || end == -1) return new ChatResult(raw.trim(), null, null);
      JsonNode node = mapper.readTree(raw.substring(start, end + 1));
      String reply = node.path("reply").asText(raw.trim());
      JsonNode swap = node.path("swap");
      if (!swap.isMissingNode() && !swap.isNull()) {
        String mealType = swap.path("mealType").asText(null);
        String foodName = swap.path("foodName").asText(null);
        if (mealType != null && foodName != null) {
          return new ChatResult(reply, mealType.toUpperCase(), foodName);
        }
      }
      return new ChatResult(reply, null, null);
    } catch (Exception e) {
      return new ChatResult(raw.trim(), null, null);
    }
  }

  private String loadPrompt(String path) throws IOException {
    try (InputStream is = new ClassPathResource(path).getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
