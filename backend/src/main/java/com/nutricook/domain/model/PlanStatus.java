package com.nutricook.domain.model;

/** Plan lifecycle states (BR-LIF-01). Transitions are decided by {@code PlanLifecycle}. */
public enum PlanStatus {
  ACTIVE,
  SUPERSEDED,
  ARCHIVED
}
