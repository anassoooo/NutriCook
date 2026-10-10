package com.nutricook.domain.plan;

import com.nutricook.domain.model.Slot;
import java.time.LocalDate;
import java.util.SplittableRandom;

/**
 * Deterministic randomness for plan composition (BR-CMP-07). The same (user, date, generation)
 * always gives the same seed, and each slot gets its own stream so slots are independent.
 */
public final class PlanSeed {

  private PlanSeed() {}

  public static long of(long userId, LocalDate date, int generationSeq) {
    long h = mix(userId);
    h = mix(h ^ date.toEpochDay());
    return mix(h ^ generationSeq);
  }

  /** The random stream for one slot of one composition attempt. */
  static SplittableRandom streamFor(long seed, int dayAttempt, Slot slot) {
    return new SplittableRandom(mix(seed ^ mix(31L * dayAttempt + slot.ordinal() + 1)));
  }

  /** SplitMix64 finalizer: a fixed, well-distributed 64-bit mix (no JVM-dependent hashing). */
  static long mix(long z) {
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
