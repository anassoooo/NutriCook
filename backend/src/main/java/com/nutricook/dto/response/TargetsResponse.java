package com.nutricook.dto.response;

import com.nutricook.domain.model.Slot;
import com.nutricook.domain.model.SlotTarget;
import com.nutricook.domain.model.Targets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Daily targets as served to clients (BR-PRF-08). {@code bmr}, {@code tdee} and {@code
 * floorApplied} let the UI explain where the numbers come from; {@code slots} gives each meal's
 * share so the client never derives targets itself (Constitution VIII).
 */
public record TargetsResponse(
    int kcal,
    int proteinG,
    int carbsG,
    int fatG,
    int fiberG,
    double bmr,
    double tdee,
    boolean floorApplied,
    List<SlotTargetResponse> slots) {

  public record SlotTargetResponse(
      Slot slot, double share, int kcal, int proteinG, int carbsG, int fatG, int fiberG) {

    static SlotTargetResponse of(SlotTarget s) {
      return new SlotTargetResponse(
          s.slot(), s.share(), s.kcal(), s.proteinG(), s.carbsG(), s.fatG(), s.fiberG());
    }
  }

  public static TargetsResponse of(Targets t, Map<Slot, SlotTarget> perSlot) {
    return new TargetsResponse(
        t.kcal(),
        t.proteinG(),
        t.carbsG(),
        t.fatG(),
        t.fiberG(),
        twoDecimals(t.bmr()),
        twoDecimals(t.tdee()),
        t.floorApplied(),
        perSlot.values().stream().map(SlotTargetResponse::of).toList());
  }

  private static double twoDecimals(double v) {
    return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
