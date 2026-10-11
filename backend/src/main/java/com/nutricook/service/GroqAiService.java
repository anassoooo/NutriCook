package com.nutricook.service;

import com.nutricook.ai.PromptLibrary;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.Meal;
import com.nutricook.entity.UserProfile;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
  private final MeterRegistry meters;

  /** A short tip for one meal, or null if the AI is unavailable. */
  public String getMealSuggestion(Meal meal, UserProfile profile) {
    try {
      return complete(
          "meal-suggestion",
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
          "plan-explanation",
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
   *
   * <p>Logs one line per call with the prompt name, outcome and latency, and records the {@code
   * nutricook.ai.calls} timer. Prompt and answer texts are never logged: they hold user data.
   *
   * @param promptName the prompt file, for logs and metrics
   */
  public String complete(String promptName, String renderedPrompt) {
    long start = System.nanoTime();
    String outcome = "error";
    try {
      String answer =
          chatClientBuilder
              .build()
              .prompt(new Prompt(new UserMessage(renderedPrompt)))
              .call()
              .content();
      outcome = answer == null || answer.isBlank() ? "empty" : "ok";
      return answer;
    } finally {
      long nanos = System.nanoTime() - start;
      Timer.builder("nutricook.ai.calls")
          .tag("prompt", promptName)
          .tag("outcome", outcome)
          .register(meters)
          .record(nanos, java.util.concurrent.TimeUnit.NANOSECONDS);
      log.info("ai_call prompt={} outcome={} ms={}", promptName, outcome, nanos / 1_000_000);
    }
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
