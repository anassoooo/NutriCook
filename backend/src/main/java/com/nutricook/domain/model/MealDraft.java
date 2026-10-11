package com.nutricook.domain.model;

import java.util.List;
import java.util.Objects;

/** A meal under construction or validation. Its nutrients are always derived (BR-CMP-08). */
public record MealDraft(Slot slot, List<Component> components) {

  public MealDraft {
    Objects.requireNonNull(slot, "slot");
    components = List.copyOf(components);
    if (components.isEmpty()) throw new IllegalArgumentException("a meal needs >= 1 component");
  }

  /** Sum of the components, rounded at meal level (BR-CMP-09). */
  public Nutrients nutrients() {
    return Nutrients.sum(components.stream().map(Component::nutrients).toList()).rounded();
  }
}
