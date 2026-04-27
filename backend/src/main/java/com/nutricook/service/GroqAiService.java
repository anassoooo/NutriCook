package com.nutricook.service;

import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.UserProfile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
                      .reduce("none", (a, b) -> a + ", " + b));

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
                      .reduce("none", (a, b) -> a + ", " + b));

      return chatClientBuilder.build().prompt(prompt).call().content();
    } catch (Exception e) {
      log.warn("Groq plan explanation failed: {}", e.getMessage());
      return null;
    }
  }

  private String loadPrompt(String path) throws IOException {
    try (InputStream is = new ClassPathResource(path).getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
