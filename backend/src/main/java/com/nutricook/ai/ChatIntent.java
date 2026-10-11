package com.nutricook.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * What the model may return in chat (BR-AI-01/02): text, and optionally the proposal to swap one
 * food. It never carries nutrition numbers; the domain computes those. Parsed with Spring AI's
 * structured output, never with string scraping (BR-AI-04).
 */
public record ChatIntent(
    @JsonPropertyDescription("Your reply to the user, at most 3 short sentences") String reply,
    @JsonPropertyDescription("Only when the user asks to change a meal; otherwise null")
        Swap swap) {

  public record Swap(
      @JsonPropertyDescription("BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK or DINNER")
          String mealType,
      @JsonPropertyDescription("The id of one food from the list of foods the user may eat")
          Long foodId) {}
}
