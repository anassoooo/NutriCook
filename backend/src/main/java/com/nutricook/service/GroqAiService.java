package com.nutricook.service;

import com.nutricook.ai.PromptLibrary;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.UserProfile;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * The only class that talks to the LLM (Groq via Spring AI). Prompts come from {@link
 * PromptLibrary} (BR-AI-08). The text it returns is never trusted as data: plans and nutrition are
 * computed by the domain (BR-AI-01).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAiService {

  private final ChatClient.Builder chatClientBuilder;
  private final PromptLibrary prompts;

  /** A short tip for one meal, or null if the AI is unavailable. */
  public String getMealSuggestion(Meal meal, UserProfile profile) {
    try {
      return complete(
          prompts.render(
              "meal-suggestion",
              Map.of(
                  "mealName", meal.getName(),
                  "mealType", meal.getMealType().name(),
                  "calories", String.valueOf(meal.getCalories()),
                  "protein", String.valueOf(meal.getProteinG()),
                  "goal", goal(profile),
                  "restrictions", restrictions(profile))));
    } catch (Exception e) {
      log.warn("AI meal suggestion failed: {}", e.getClass().getSimpleName());
      return null;
    }
  }

  /** A short explanation of the day's plan, or null if the AI is unavailable. */
  public String getPlanExplanation(DietPlan plan, UserProfile profile) {
    try {
      return complete(
          prompts.render(
              "plan-explanation",
              Map.of(
                  "totalCalories", String.valueOf(plan.getTotalCalories()),
                  "totalProtein", String.valueOf(plan.getTotalProteinG()),
                  "goal", goal(profile),
                  "conditions",
                      profile.getHealthConditions().stream()
                          .map(c -> c.getName())
                          .collect(Collectors.joining(", ")))));
    } catch (Exception e) {
      log.warn("AI plan explanation failed: {}", e.getClass().getSimpleName());
      return null;
    }
  }

  /**
   * Sends a fully rendered prompt as-is (no further templating, so braces in format instructions
   * are safe). Exceptions propagate: callers decide how to degrade (BR-AI-05).
   */
  public String complete(String renderedPrompt) {
    return chatClientBuilder
        .build()
        .prompt(new Prompt(new UserMessage(renderedPrompt)))
        .call()
        .content();
  }

  static String goal(UserProfile profile) {
    return profile.getHealthGoal() != null ? profile.getHealthGoal().name() : "IMPROVE_HEALTH";
  }

  static String restrictions(UserProfile profile) {
    String r =
        profile.getDietaryRestrictions().stream()
            .map(x -> x.getName())
            .collect(Collectors.joining(", "));
    return r.isBlank() ? "none" : r;
  }
}
