package com.nutricook.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nutricook.ai.ChatIntent;
import com.nutricook.ai.PromptLibrary;
import com.nutricook.config.LimitsProperties;
import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.model.Targets;
import com.nutricook.dto.response.ChatResponse;
import com.nutricook.dto.response.DietPlanResponse;
import com.nutricook.entity.DietPlan;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.Meal;
import com.nutricook.entity.MealFoodItem;
import com.nutricook.entity.UserProfile;
import com.nutricook.repository.ProgressRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * One chat turn (US4). The model sees only foods the user may eat and can only <em>propose</em> a
 * swap; the proposal goes through {@link SwapService}, exactly like a manual swap (BR-AI-01..07).
 * No database transaction is held while the model is thinking.
 */
@Service
@Slf4j
public class ChatService {

  /** Message shown when the model's output could not be understood twice (BR-AI-04). */
  static final String FALLBACK_REPLY =
      "Sorry, I couldn't put that together. Could you rephrase your question?";

  private final DietPlanService plans;
  private final UserProfileService profiles;
  private final TargetService targets;
  private final SafetyService safety;
  private final SwapService swaps;
  private final GroqAiService ai;
  private final PromptLibrary prompts;
  private final ProgressRepository progress;
  private final UserClock userClock;
  private final LimitsProperties limits;
  private final Executor aiCalls;
  private final BeanOutputConverter<ChatIntent> converter =
      new BeanOutputConverter<>(ChatIntent.class);
  private final Cache<Long, Bucket> buckets =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(2)).build();

  public ChatService(
      DietPlanService plans,
      UserProfileService profiles,
      TargetService targets,
      SafetyService safety,
      SwapService swaps,
      GroqAiService ai,
      PromptLibrary prompts,
      ProgressRepository progress,
      UserClock userClock,
      LimitsProperties limits,
      @Qualifier("aiCallExecutor") Executor aiCalls) {
    this.plans = plans;
    this.profiles = profiles;
    this.targets = targets;
    this.safety = safety;
    this.swaps = swaps;
    this.ai = ai;
    this.prompts = prompts;
    this.progress = progress;
    this.userClock = userClock;
    this.limits = limits;
    this.aiCalls = aiCalls;
  }

  public ChatResponse chat(Long userId, Long planId, String message) {
    consumeQuota(userId);

    DietPlan plan = plans.getByIdForUser(userId, planId);
    UserProfile profile = profiles.getByUserId(userId);
    List<FoodItem> eligible = safety.eligibleFoods(safety.constraintsFor(profile));
    Set<Long> candidateIds = eligible.stream().map(FoodItem::getId).collect(Collectors.toSet());

    String prompt = prompts.render("chat", variables(userId, plan, profile, eligible, message));
    ChatIntent intent = ask(prompt + "\n\n" + converter.getFormat());

    String reply = intent.reply();
    DietPlanResponse updatedPlan = null;
    ChatIntent.Swap swap = intent.swap();
    if (swap != null && swap.foodId() != null && swap.mealType() != null) {
      try {
        // BR-AI-02: whatever the model says, only foods from the candidate list are accepted
        if (!candidateIds.contains(swap.foodId())) {
          throw new DomainException(
              ErrorCode.SWAP_REJECTED, "that food isn't available with your restrictions.");
        }
        Meal meal = mealOfType(plan, swap.mealType());
        DietPlan swapped = swaps.swap(userId, planId, meal.getId(), swap.foodId());
        updatedPlan = DietPlanResponse.of(swapped, plans.disclaimerFor(userId));
      } catch (DomainException e) {
        reply = reply + "\n\n(I couldn't make that swap: " + lowerFirst(e.getMessage()) + ")";
      }
    }
    return ChatResponse.builder().reply(reply).updatedPlan(updatedPlan).build();
  }

  /** BR-AI-07: per-user messages per rolling hour (token bucket, greedy refill). */
  private void consumeQuota(Long userId) {
    int perHour = limits.chatPerHour();
    Bucket bucket =
        buckets.get(
            userId,
            id ->
                Bucket.builder()
                    .addLimit(
                        Bandwidth.builder()
                            .capacity(perHour)
                            .refillGreedy(perHour, Duration.ofHours(1))
                            .build())
                    .build());
    if (!bucket.tryConsume(1)) {
      throw new DomainException(
          ErrorCode.RATE_LIMITED,
          "You can send " + perHour + " messages per hour. Please wait a little.",
          Map.of("limit", perHour));
    }
  }

  /**
   * BR-AI-04: structured output, retried once; then a plain-text reply with no intent. BR-AI-05: a
   * timeout or provider error is AI_UNAVAILABLE, never a 500.
   */
  private ChatIntent ask(String prompt) {
    String raw = callModel(prompt);
    ChatIntent parsed = parse(raw);
    if (parsed != null) return parsed;
    log.info("Chat output not parseable; retrying once");
    String retry = callModel(prompt);
    parsed = parse(retry);
    return parsed != null ? parsed : new ChatIntent(plainText(retry), null);
  }

  private ChatIntent parse(String raw) {
    try {
      ChatIntent intent = converter.convert(raw);
      return intent != null && intent.reply() != null && !intent.reply().isBlank() ? intent : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  private String callModel(String prompt) {
    try {
      return CompletableFuture.supplyAsync(() -> ai.complete("chat", prompt), aiCalls)
          .get(limits.aiTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw unavailable();
    } catch (Exception e) {
      log.warn("Chat AI call failed: {}", e.getClass().getSimpleName());
      throw unavailable();
    }
  }

  private static DomainException unavailable() {
    return new DomainException(
        ErrorCode.AI_UNAVAILABLE, "The AI assistant is unavailable right now. Please try again.");
  }

  /** Free text the model sent instead of JSON, if it reads like an answer. */
  static String plainText(String raw) {
    if (raw == null) return FALLBACK_REPLY;
    String text = raw.strip();
    if (text.isEmpty() || text.startsWith("{") || text.startsWith("```")) return FALLBACK_REPLY;
    return text.length() > 1000 ? text.substring(0, 1000) : text;
  }

  private Map<String, Object> variables(
      Long userId, DietPlan plan, UserProfile profile, List<FoodItem> eligible, String message) {
    LocalDate today = userClock.today(profile.getTimezone());
    Map<String, Object> v = new HashMap<>();
    v.put("date", plan.getValidForDate().toString());
    v.put("plan", describe(plan));
    v.put("goal", GroqAiService.goal(profile));
    v.put("targets", describe(targets.forProfile(profile)));
    v.put("restrictions", GroqAiService.restrictions(profile));
    v.put(
        "conditions",
        profile.getHealthConditions().isEmpty()
            ? "none"
            : profile.getHealthConditions().stream()
                .map(c -> c.getName())
                .collect(Collectors.joining(", ")));
    v.put(
        "progress",
        progress
            .findByUserIdAndDate(userId, today)
            .map(
                p ->
                    "%d kcal eaten, %d ml water, %d min exercise"
                        .formatted(
                            orZero(p.getCaloriesConsumed()),
                            orZero(p.getWaterMl()),
                            orZero(p.getExerciseMinutes())))
            .orElse("nothing yet"));
    v.put(
        "candidates",
        eligible.stream()
            .sorted(Comparator.comparing(FoodItem::getId))
            .map(
                f ->
                    f.getId()
                        + ": "
                        + f.getName()
                        + ", "
                        + f.getRole().name().toLowerCase(Locale.ROOT))
            .collect(Collectors.joining("\n")));
    v.put("message", sanitize(message));
    return v;
  }

  /** BR-AI-06: the user's text cannot close the delimiter it is wrapped in. */
  static String sanitize(String message) {
    return message.replaceAll("(?i)</?\\s*user_message\\s*>", "").strip();
  }

  private static String describe(DietPlan plan) {
    return plan.getMeals().stream()
        .sorted(Comparator.comparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
        .map(
            m ->
                "- "
                    + m.getMealType().name()
                    + " ("
                    + m.getCalories()
                    + " kcal): "
                    + m.getMealFoodItems().stream()
                        .map(ChatService::describe)
                        .collect(Collectors.joining(", ")))
        .collect(Collectors.joining("\n"));
  }

  private static String describe(MealFoodItem item) {
    return item.getFoodItem().getName()
        + " "
        + Math.round(item.getQuantityG())
        + " g ("
        + item.getRole().name().toLowerCase(Locale.ROOT)
        + ")";
  }

  private static String describe(Targets t) {
    return "%d kcal, %d g protein, %d g carbs, %d g fat"
        .formatted(t.kcal(), t.proteinG(), t.carbsG(), t.fatG());
  }

  private static Meal mealOfType(DietPlan plan, String mealType) {
    return plan.getMeals().stream()
        .filter(m -> m.getMealType().name().equalsIgnoreCase(mealType.trim()))
        .findFirst()
        .orElseThrow(
            () -> new DomainException(ErrorCode.SWAP_REJECTED, "that meal is not in your plan."));
  }

  private static int orZero(Integer v) {
    return v == null ? 0 : v;
  }

  private static String lowerFirst(String s) {
    return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }
}
