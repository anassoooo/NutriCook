# NutriCook — Data Model

**Version**: 2.0.0 | **Date**: 2026-10-11 | **Schema**: Flyway V1–V8 + `R__food_catalog`

> The schema belongs to the Flyway migrations in `backend/src/main/resources/db/migration`
> (Constitution VI); Hibernate only validates it. This page is a reading aid: the migrations win
> when they disagree. Rules are referenced by their IDs in
> [`docs/domain/business-rules.md`](../domain/business-rules.md).

---

## 1. Entity–relationship diagram

```mermaid
erDiagram
    users ||--|| user_profiles : "has"
    users ||--o{ diet_plans : "owns"
    users ||--o{ progress_entries : "logs"
    users ||--o{ user_achievements : "earns"
    achievements ||--o{ user_achievements : "awarded as"

    user_profiles }o--o{ health_conditions : "user_health_conditions"
    user_profiles }o--o{ dietary_restrictions : "user_dietary_restrictions"
    health_conditions ||--o{ condition_implied_allergens : "implies"
    dietary_restrictions ||--o{ restriction_excluded_allergens : "excludes"

    diet_plans ||--|{ meals : "5 slots"
    meals ||--|{ meal_food_items : "components"
    food_items ||--o{ meal_food_items : "snapshotted in"
    food_items ||--o{ food_allergens : ""
    food_items ||--o{ food_diet_flags : ""
    food_items ||--o{ food_suitable_slots : ""

    users {
        bigint id PK
        varchar email UK
        varchar password_hash "BCrypt"
        varchar role "USER | ADMIN"
    }
    user_profiles {
        bigint id PK
        bigint user_id FK,UK
        date date_of_birth
        varchar gender
        float weight_kg "updated by the latest weigh-in (BR-PRG-04)"
        float height_cm
        varchar activity_level
        varchar health_goal
        varchar timezone "IANA zone, defines the user's day (BR-TIM-01)"
    }
    health_conditions {
        bigint id PK
        varchar name UK
        float sugar_limit_per_meal_g
        float fat_limit_per_meal_g
        float sodium_limit_per_day_mg
    }
    dietary_restrictions {
        bigint id PK
        varchar name UK
        varchar type "ALLERGY | DIET | ..."
        varchar required_diet_flag "e.g. VEGAN (BR-SAF-10)"
    }
    food_items {
        bigint id PK
        varchar name "unique, case-insensitive"
        varchar role "PROTEIN | CARB | PRODUCE | FAT"
        varchar state "RAW | COOKED | DRY (BR-CAT-03)"
        float calories_per100g
        float protein_per100g
        float carbs_per100g
        float fat_per100g
        float sugar_per100g
        float fiber_per100g
        float sodium_mg_per100g
        float portion_min_g
        float portion_max_g
        float portion_step_g
        float default_portion_g
        varchar source "dataset, e.g. USDA SR Legacy"
        boolean active "soft delete (BR-CAT-04)"
        boolean allergens_known "false fails closed (BR-SAF-03)"
        boolean meat
        varchar origin "SEED | ADMIN (BR-CAT-05)"
    }
    diet_plans {
        bigint id PK
        bigint user_id FK
        date valid_for_date "the user's day"
        varchar status "ACTIVE | SUPERSEDED | ARCHIVED"
        int generation_seq "1, 2, ... per day"
        bigint seed "reproducible composition (BR-CMP-07)"
        int target_calories "what the plan was built for (BR-PRG-06)"
        int total_calories
        jsonb warnings "e.g. TARGET_DEVIATION"
        varchar ai_status "PENDING | READY | UNAVAILABLE"
        text ai_explanation "optional, async (BR-LIF-07)"
    }
    meals {
        bigint id PK
        bigint diet_plan_id FK
        varchar meal_type "5 slots"
        int calories
        text ai_suggestion
        boolean completed "checked off (BR-PRG-05)"
        timestamp completed_at
    }
    meal_food_items {
        bigint id PK
        bigint meal_id FK
        bigint food_item_id FK
        float quantityg
        varchar role
        float kcal "snapshot: catalog edits never rewrite history (BR-LIF-05)"
        float protein_g
        float carbs_g
        float fat_g
    }
    progress_entries {
        bigint id PK
        bigint user_id FK
        date date "one per user and day (BR-PRG-01)"
        float weight_kg
        int calories_consumed "derived unless overridden"
        boolean calories_overridden
        int water_ml
        int meals_completed "derived"
        int exercise_minutes
        int target_calories "snapshot (BR-PRG-06)"
        text notes
    }
    achievements {
        bigint id PK
        varchar name UK
        text description "states the exact rule (BR-ACH-04)"
        varchar type
        int threshold
    }
    user_achievements {
        bigint id PK
        bigint user_id FK
        bigint achievement_id FK
        timestamp earned_at
    }
```

The five collection tables (`food_allergens`, `food_diet_flags`, `food_suitable_slots`,
`condition_implied_allergens`, `restriction_excluded_allergens`) hold one enum value per row.

---

## 2. Constraints that enforce business rules

The application checks every rule first; these constraints are the last line of defense
(defense in depth), so even a bug or a manual edit cannot store an invalid state.

| Constraint | Table | Rule |
|---|---|---|
| `ux_active_plan` (partial unique on `user_id, valid_for_date` where `ACTIVE`) | `diet_plans` | One ACTIVE plan per user-day, even under concurrent generation (BR-LIF-02) |
| `ck_diet_plans_status`, `ck_diet_plans_ai_status` | `diet_plans` | Lifecycle states (BR-LIF-01, BR-LIF-07) |
| `uq_user_progress_date` | `progress_entries` | One entry per user and day (BR-PRG-01) |
| `ck_progress_*` (calories, water, exercise, weight, notes, meals, target) | `progress_entries` | Bounds (BR-PRG-03) |
| `ck_meals_completed_at` | `meals` | `completed_at` only on checked meals (BR-PRG-05) |
| `uq_user_achievement` | `user_achievements` | Awards are unique; inserts use `ON CONFLICT DO NOTHING` (BR-ACH-06) |
| `ck_achievements_type` | `achievements` | Only measurable types (BR-ACH-05) |
| `uq_food_items_name`, `ux_food_items_name_ci` | `food_items` | Unique names, ignoring case (BR-CAT-01) |
| `ck_food_items_origin` | `food_items` | SEED or ADMIN (BR-CAT-05) |
| `ck_mfi_role` | `meal_food_items` | Component roles (BR-CMP-02) |

---

## 3. Migration history

| Migration | What it does |
|---|---|
| `V1__baseline` | The original schema, captured as the first migration |
| `V2__profile_timezone` | `user_profiles.timezone`: "today" is the user's day (BR-TIM-01) |
| `V3__food_attributes` | Food roles, states, portions, allergens, diet flags, slots and sources (BR-CAT, BR-SAF) |
| `V4__reference_data` | Restrictions, conditions and achievements as data, with the allergens they exclude or imply |
| `V5__plan_lifecycle` | SUPERSEDED status, `ux_active_plan`, seed and sequence, warnings, AI status, component snapshots |
| `V6__progress_meal_completion` | Meal check-off, plan target, progress override and target snapshot, progress bounds |
| `V7__achievement_types` | `LOGGING_STREAK`, PROTEIN_GOAL removed, rule-stating descriptions, weight milestones |
| `V8__food_origin` | `origin` SEED/ADMIN, case-insensitive unique names |
| `R__food_catalog` | The curated catalog, generated from `db/catalog/foods.csv`; touches SEED rows only |

Every versioned migration was also applied to a database holding earlier data, not only to an
empty one, and the upgraded rows were checked.
