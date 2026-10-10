# NutriCook — Business Rules Catalogue

> **Status**: Reference · **Version**: 1.2.0 · **Last updated**: 2026-10-10
> **Authority**: Constitution Principle I (Business Rules Are the Product). When code and this
> document disagree, the code is wrong until this document is amended.

Every rule has a stable ID (`BR-<AREA>-<NN>`). IDs are never reused or renumbered. Each rule
MUST be covered by at least one automated test whose name starts with the rule ID
(e.g. `BR_NRG_01_bmrUsesMifflinStJeor`). The **Today** column shows how the code at commit
`39679a0` compares to the rule:

- ✅ **OK**: implemented as written
- ⚠️ **DIVERGES**: something exists but behaves differently
- ❌ **MISSING**: not implemented

Areas: [Profile](#1-profile--eligibility-br-prf) · [Energy](#2-energy-target-br-nrg) ·
[Macros](#3-macronutrient-targets-br-mac) · [Safety](#4-food-safety--eligibility-br-saf) ·
[Composition](#5-plan-composition-br-cmp) · [Lifecycle](#6-plan-lifecycle-br-lif) ·
[AI](#7-ai-assistant-br-ai) · [Progress](#8-progress-tracking-br-prg) ·
[Achievements](#9-achievements-br-ach) · [Catalog](#10-food-catalog-br-cat) ·
[Time](#11-time--calendar-br-tim) · [Errors](#12-error-codes) · [Constants](#13-configurable-constants)

---

## Glossary

| Term | Definition |
|---|---|
| **BMR** | Basal Metabolic Rate (kcal/day) at rest |
| **TDEE** | Total Daily Energy Expenditure = BMR × activity factor |
| **Target** | The daily calorie and macro goals the user's plans are built to hit |
| **Hard constraint** | A rule that must never be broken. If it can't be met, the operation fails ("fail closed") |
| **Soft target** | A goal with a tolerance. Small misses produce a warning; large misses fail |
| **Eligible food** | A food that passes every hard constraint for a given user |
| **Slot** | One of the five meal types (BREAKFAST … DINNER) and its share of the day's calories |
| **Component** | One food item and its portion inside a meal, with a role (PROTEIN, CARB, PRODUCE, FAT) |
| **Snapshot** | A stored value that is never recomputed from data that changes later |
| **User day** | A calendar date in the user's own timezone (BR-TIM-01) |

---

## 1. Profile & eligibility (BR-PRF)

| ID | Rule | Today |
|---|---|---|
| BR-PRF-01 | Computing targets or generating a plan **requires** these profile fields: `dateOfBirth`, `gender`, `weightKg`, `heightCm`, `activityLevel`, `healthGoal`. A field that is missing, or holds a stored value outside the BR-PRF-03 ranges (data entered before those ranges existed), is listed in `missingFields` of a `PROFILE_INCOMPLETE` error. There is no silent default. | ✅ |
| BR-PRF-02 | Plans are generated only for users aged **18–100** (exact age, BR-TIM-02). Users outside that range get `PROFILE_INELIGIBLE`. They can still log progress. | ✅ |
| BR-PRF-03 | Accepted ranges: weight **30–300 kg**, height **120–230 cm**, date of birth in the past. Values outside them get `VALIDATION_FAILED`. | ✅ |
| BR-PRF-04 | Unknown enum values (sex, activity, goal) get `VALIDATION_FAILED` listing the allowed values. They are never a 500. | ✅ |
| BR-PRF-05 | Profile updates are **partial**: fields left out are not changed. A field present in the request is validated. | ✅ |
| BR-PRF-06 | Profile changes affect **future** plans only. Existing plans are snapshots (BR-LIF-05). | ✅ |
| BR-PRF-07 | A health condition may **imply** restrictions: *Celiac Disease* ⇒ excludes `GLUTEN`; *Lactose Intolerance* ⇒ excludes `MILK`. Implied restrictions apply even if the user didn't add the matching restriction. | ✅ |
| BR-PRF-08 | The profile response includes the computed **targets** (BR-NRG, BR-MAC). Clients must never hard-code targets. | ✅ |

## 2. Energy target (BR-NRG)

| ID | Rule | Today |
|---|---|---|
| BR-NRG-01 | BMR uses **Mifflin-St Jeor**: `BMR = 10·W + 6.25·H − 5·A + s`, with W in kg, H in cm, A in exact years. `s = +5` (MALE), `−161` (FEMALE), `−78` (OTHER / PREFER_NOT_TO_SAY, the midpoint). | ✅ |
| BR-NRG-02 | `TDEE = BMR × f`, where f = 1.2 / 1.375 / 1.55 / 1.725 / 1.9 for SEDENTARY … EXTRA_ACTIVE. | ✅ |
| BR-NRG-03 | Goal adjustment: LOSE_WEIGHT **−500**, GAIN_MUSCLE **+300**, MAINTAIN_WEIGHT / IMPROVE_HEALTH **0** kcal. | ✅ |
| BR-NRG-04 | Floor: `floor = max(sexMinimum, BMR)`, where sexMinimum = 1500 (MALE) or 1200 (others). A deficit never brings intake below BMR. | ✅ |
| BR-NRG-05 | `target = roundToNearest10(max(TDEE + adjustment, floor))`. If that rounding lands below `floor`, round **up** to the next multiple of 10 instead, so rounding never undoes BR-NRG-04. | ✅ |
| BR-NRG-06 | The energy calculation is a **pure function** of (profile, date). It has no I/O, no randomness, and no call to `now()`. | ✅ |

**Reference cases** (these are acceptance tests and MUST pass exactly):

| # | Sex | Age | kg | cm | Activity | Goal | BMR | TDEE | Target kcal | Protein g | Fat g | Carbs g | Fiber g |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A | M | 30 | 80 | 180 | MODERATELY | MAINTAIN | 1780.00 | 2759.00 | **2760** | 96 | 92 | 387 | 39 |
| B | F | 25 | 60 | 165 | LIGHTLY | LOSE | 1345.25 | 1849.72 | **1350** | 108 | 45 | 128 | 19 |
| C | F | 60 | 50 | 155 | SEDENTARY | LOSE | 1007.75 | 1209.30 | **1200** (floor) | 90 | 40 | 120 | 17 |
| D | M | 20 | 70 | 175 | VERY | GAIN | 1698.75 | 2930.34 | **3230** | 140 | 108 | 425 | 45 |

## 3. Macronutrient targets (BR-MAC)

| ID | Rule | Today |
|---|---|---|
| BR-MAC-01 | Energy per gram (Atwater factors): protein 4, carbohydrate 4, fat 9 kcal/g. | ✅ |
| BR-MAC-02 | Protein (g) = `perKg × W`. perKg: LOSE 1.8, GAIN 2.0, MAINTAIN 1.2, IMPROVE 1.0. Capped at `min(2.2·W, 0.35·target/4)`. | ✅ |
| BR-MAC-03 | Fat (g) = 30% of target ÷ 9, and never below `0.6·W`. | ✅ |
| BR-MAC-04 | Carbs (g) = `(target − 4·protein − 9·fat) ÷ 4`, worked out from unrounded values. A negative result is a defect, not a valid outcome. | ✅ |
| BR-MAC-05 | Fiber (g) = 14 g per 1000 kcal of target (DGA). | ✅ |
| BR-MAC-06 | Each macro gram value is rounded to the nearest integer once, at the very end. | ✅ |
| BR-MAC-07 | `sugarG` in the catalog means **total** sugars. The app MUST NOT label it "added sugar" or apply the WHO 10%-added-sugar limit to it. A sugar ceiling exists only when a health condition sets one (BR-SAF-06). | ✅ |

## 4. Food safety & eligibility (BR-SAF)

**These are hard constraints. Every rule in this section fails closed.**

| ID | Rule | Today |
|---|---|---|
| BR-SAF-01 | Each food declares its **allergens** using the EU 14 list (GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS, SOY, MILK, TREE_NUTS, CELERY, MUSTARD, SESAME, SULPHITES, LUPIN, MOLLUSCS) and its **diet flags** (VEGAN, VEGETARIAN, HALAL, KOSHER). Eligibility is decided from these attributes **only**, never from the category string. | ✅ |
| BR-SAF-02 | Restriction → exclusion mapping: Nut Allergy ⇒ PEANUTS + TREE_NUTS; Gluten-Free ⇒ GLUTEN; Lactose-Free ⇒ MILK; Shellfish Allergy ⇒ CRUSTACEANS + MOLLUSCS; Egg Allergy ⇒ EGGS; Soy Allergy ⇒ SOY; Fish Allergy ⇒ FISH; Sesame Allergy ⇒ SESAME; Vegan ⇒ must have VEGAN; Vegetarian ⇒ must have VEGETARIAN; Halal ⇒ must have HALAL; Kosher ⇒ must have KOSHER. The mapping is data (`restriction_excluded_allergens`, `required_diet_flag`), never name matching. | ✅ |
| BR-SAF-03 | A food with **unknown** allergen data counts as ineligible for any user with an allergy or intolerance. | ✅ |
| BR-SAF-04 | If the eligible pool can't fill a required component role, generation fails with `PLAN_INFEASIBLE` and names the constraint. It **never** falls back to ineligible foods. | ✅ |
| BR-SAF-05 | Every path that changes a plan goes through the same `PlanSafetyValidator` before anything is saved: generation, manual swap, AI swap, admin edits. A violation throws and nothing is persisted. | ✅ |
| BR-SAF-06 | Health-condition nutrient limits are hard constraints. Diabetes: total sugar ≤ 15 g per meal. High Cholesterol: fat ≤ 25 g per meal. Hypertension: sodium ≤ 2000 mg per **day**. | ✅ |
| BR-SAF-07 | The list of foods offered to the AI as swap candidates is the user's **eligible** pool, never the full catalog. | ✅ |
| BR-SAF-08 | Responses for users with a health condition carry `disclaimer: "NOT_MEDICAL_ADVICE"`. The UI shows it next to the plan. | ✅ |
| BR-SAF-09 | Kosher: a meal must not combine a meat component (role PROTEIN, category meat) with any MILK-containing component. | ✅ |
| BR-SAF-10 | Religious and diet flags fail closed: a flag is set only when it holds without certification doubts. Land-animal meat is HALAL / KOSHER only when the item is explicitly certified (e.g. "Chicken Breast (halal)"). Fresh fish with fins and scales is HALAL and KOSHER; canned fish is HALAL only (KOSHER needs certification); shellfish and molluscs are neither (disputed / forbidden). Cheese carries no VEGETARIAN / HALAL / KOSHER flag (animal rennet). Single-ingredient plant foods are VEGAN, VEGETARIAN, HALAL and KOSHER; processed plant foods (bread, tortilla, hummus, nut butters, tofu) are not KOSHER without certification, and bread is not VEGAN (may contain milk or honey). | ✅ |

## 5. Plan composition (BR-CMP)

| ID | Rule | Today |
|---|---|---|
| BR-CMP-01 | Slot shares: BREAKFAST 25%, MORNING_SNACK 10%, LUNCH 30%, AFTERNOON_SNACK 10%, DINNER 25%. They must add up to exactly 100%; the app checks this at startup. | ✅ |
| BR-CMP-02 | **Main meals** (BREAKFAST, LUNCH, DINNER) have exactly one PROTEIN, one CARB and one PRODUCE component, plus an optional FAT component (added only to close an energy gap). **Snacks** have two components: PROTEIN + PRODUCE or PRODUCE + FAT. Portion sizing: PRODUCE uses its default portion; in main meals, PROTEIN is sized to the day's protein target split across the main meals by their slot shares, CARB fills the remaining energy, and FAT closes what is left. In snacks, the non-produce component fills the slot's energy. | ⚠️ every meal is a single random food (e.g. 600 kcal of cucumber ≈ 3.7 kg) |
| BR-CMP-03 | Every portion stays inside its food's `[portionMinG, portionMaxG]` and is rounded to `portionStepG` (default 5 g). | ❌ unbounded |
| BR-CMP-04 | A food is used only in slots listed in its `suitableSlots` (salmon is not a breakfast snack). | ❌ |
| BR-CMP-05 | Tolerances. Day calories within **±5%** of target → OK. Within ±10% → OK with a `TARGET_DEVIATION` warning. Beyond ±10% → `PLAN_INFEASIBLE`. Protein ≥ 90% of target, otherwise a `LOW_PROTEIN` warning. Each meal aims for ±15% of its slot share; candidates within that band are preferred. When the day is outside ±5%, CARB portions (then FAT) are scaled once within their bounds, keeping a meal's previous portions if the scaled meal would break a safety limit. | ❌ |
| BR-CMP-06 | Variety: one food is used at most twice in a day, and a PROTEIN food at most once. | ❌ |
| BR-CMP-07 | **Deterministic**: the seed is a fixed 64-bit mix of `(userId, date, generationSeq)`. Each slot draws from its own stream (`SplittableRandom`), so changing one slot never reshuffles another. The same inputs and catalog give the same plan. | ❌ `new Random()` |
| BR-CMP-08 | Plan totals = sum of meal totals = sum of component nutrition, computed by the domain on **every** change. Totals are never set directly. | ⚠️ recomputed by hand in two places |
| BR-CMP-09 | Component nutrition = `per100g × quantityG / 100`. kcal is rounded to an integer and grams to 0.1, at the meal level. | ✅ |

## 6. Plan lifecycle (BR-LIF)

| ID | Rule | Today |
|---|---|---|
| BR-LIF-01 | Plan states are `ACTIVE`, `SUPERSEDED` and `ARCHIVED`. Allowed transitions: ACTIVE→SUPERSEDED (regenerated) and ACTIVE→ARCHIVED (user archives). Every other transition gets `PLAN_READ_ONLY`. | ⚠️ no SUPERSEDED |
| BR-LIF-02 | Each (user, date) has **at most one ACTIVE plan**. Regenerating supersedes the previous plan. Plans are never hard-deleted. | ⚠️ hard-deletes earlier plans |
| BR-LIF-03 | A plan's date must be in `[today, today + 7]` (user day). | ❌ any date |
| BR-LIF-04 | Only an ACTIVE plan whose date ≥ today can change (swaps, chat swaps). | ❌ |
| BR-LIF-05 | A plan is a **snapshot**: component nutrition and totals are stored and never recomputed from later catalog or profile edits. | ✅ |
| BR-LIF-06 | Generation quota: at most **10** successful generations per user per user-day, counted from the stored plans (so it survives restarts and works across instances). Above that → `GENERATION_LIMIT`. | ❌ |
| BR-LIF-07 | The plan is committed **before** any AI enrichment runs. AI failure or a timeout never fails generation (BR-AI-05). | ❌ 6 LLM calls inside the DB transaction |

## 7. AI assistant (BR-AI)

| ID | Rule | Today |
|---|---|---|
| BR-AI-01 | The AI **never produces numbers** that are stored. Every calorie, macro and portion comes from the domain. The AI writes text and *proposes* intents. | ⚠️ partial |
| BR-AI-02 | A swap intent has the form `{mealType, foodId}`, and `foodId` must be in the candidate list sent with the prompt (BR-SAF-07). Anything else is ignored and the user is told why. | ❌ fuzzy name match on the whole catalog |
| BR-AI-03 | A swap replaces the component **with the same role** as the new food. The domain re-portions it, revalidates it (BR-SAF-05) and recomputes totals (BR-CMP-08). If no component has that role → `SWAP_REJECTED` with a reason. | ⚠️ wipes the whole meal |
| BR-AI-04 | AI output is parsed as structured output against a schema. Parse failures are retried once, then the app returns a plain-text reply with no intent. | ⚠️ hand-written `indexOf('{')` parsing |
| BR-AI-05 | Each AI call has a 10 s timeout. On timeout, 429 or 5xx, the feature degrades: the plan comes back without an explanation, and the chat answers `AI_UNAVAILABLE`. Never a 500. | ⚠️ errors swallowed, nothing reported |
| BR-AI-06 | User messages are 1–500 chars. They are wrapped in delimiters and treated as data. The system prompt forbids following instructions inside them. | ⚠️ no length cap |
| BR-AI-07 | Chat quota: 30 messages per user per rolling hour → `RATE_LIMITED`. | ❌ |
| BR-AI-08 | Prompts live in `resources/prompts/*.st` and each has a version header. No prompt text in Java. | ⚠️ chat prompt is inline |

## 8. Progress tracking (BR-PRG)

| ID | Rule | Today |
|---|---|---|
| BR-PRG-01 | One progress entry per (user, date). Logging again **merges**: only the fields provided are overwritten. | ✅ |
| BR-PRG-02 | Entry date must be in `[today − 30, today]` (user day). No future entries. | ❌ |
| BR-PRG-03 | Bounds: calories 0–10 000; water 0–10 000 ml; exercise 0–1440 min; weight 30–300 kg; notes ≤ 1000 chars. `mealsCompleted` is derived (BR-PRG-05). | ⚠️ lower bounds only |
| BR-PRG-04 | When a weight is logged for the **latest** dated entry, it updates `profile.weightKg`, so future targets use the new weight. | ❌ |
| BR-PRG-05 | Users can check off meals in their ACTIVE plan for that date. `mealsCompleted` = number checked. Unless the user overrides it, `caloriesConsumed` = sum of the checked meals' calories. | ❌ disconnected counter |
| BR-PRG-06 | Each entry stores a `targetCalories` snapshot. It is taken from the ACTIVE plan for that date, or from the profile target if there is no plan. Achievements use it (BR-ACH-03). | ❌ |

## 9. Achievements (BR-ACH)

| ID | Rule | Today |
|---|---|---|
| BR-ACH-01 | **Logging streak**: the number of consecutive days with an entry, ending today. If today has no entry yet, the streak ends **yesterday** (grace until the end of the user-day). | ⚠️ resets to 0 every morning |
| BR-ACH-02 | **Plans generated** counts distinct (user, date) plans, so superseded regenerations don't count. | ⚠️ counts every row |
| BR-ACH-03 | **Calorie goal day**: `|caloriesConsumed − targetCalories| ≤ 10% × targetCalories` and `caloriesConsumed > 0`. Counted over a rolling 30 days. | ⚠️ any calories > 0 counts |
| BR-ACH-04 | **Hydration day**: water ≥ 2000 ml. Counted over a rolling 30 days. Each achievement description must state the exact rule. | ⚠️ description says "7 days" without saying how they're counted |
| BR-ACH-05 | Every `AchievementType` has exactly one evaluator, and a test fails if one is missing. Types that can't be measured yet (PROTEIN_GOAL) are removed until protein intake is tracked. WEIGHT_MILESTONE = net weight change of `threshold` kg in the goal's direction since the first logged weight. | ⚠️ PROTEIN_GOAL and WEIGHT_MILESTONE silently never fire |
| BR-ACH-06 | Awards are **idempotent** (unique per user and achievement; a duplicate insert is a no-op) and **permanent** (never revoked, even if data is later edited). | ⚠️ unique constraint exists; a race gives a 500 |
| BR-ACH-07 | Evaluation runs after a progress log, a meal check-off or a plan generation, using **one** range query (not one query per day). | ⚠️ up to 365 queries |

## 10. Food catalog (BR-CAT)

| ID | Rule | Today |
|---|---|---|
| BR-CAT-01 | Each food has: name (unique), `role`, `state` (RAW / COOKED / DRY), per-100 g kcal, protein, carbs, fat, sugar, fiber, sodium, allergens, diet flags, portion bounds, suitable slots and `source` (the dataset, e.g. `USDA SR Legacy`). The seeded catalog is maintained in `backend/src/main/resources/db/catalog/foods.csv` and applied by the repeatable migration `R__food_catalog.sql`, which `scripts/gen-food-seed.py` generates from the CSV. | ✅ |
| BR-CAT-02 | Data-quality gate: every value ≥ 0; fiber ≤ carbs; sugar ≤ carbs; protein + carbs + fat ≤ 100; energy matches the macros: `|kcal − (4P + 4(C − fiber) + 2·fiber + 9F)| ≤ max(15% × kcal, 10 kcal)`. Fiber counts 2 kcal/g (EU Regulation 1169/2011), otherwise correct vegetable data fails; the 10 kcal floor keeps very low-energy foods testable. A failing food is rejected on admin save and fails the catalog test. | ⚠️ gate + catalog test done; admin save arrives with US7 |
| BR-CAT-03 | Values match their declared `state`. Mixing dry and cooked values with no label is a defect. | ✅ |
| BR-CAT-04 | Foods are **soft-deleted** (`active = false`). Inactive foods are excluded from new plans but still show in historical ones (BR-LIF-05). | ✅ |
| BR-CAT-05 | Only `ADMIN` can create, update or deactivate foods. | ❌ no admin API |

## 11. Time & calendar (BR-TIM)

| ID | Rule | Today |
|---|---|---|
| BR-TIM-01 | Each user has an IANA `timezone`, captured from the browser at onboarding (default `UTC`). "Today" always means the user's today. | ✅ |
| BR-TIM-02 | Age is computed as `Period.between(dateOfBirth, today).getYears()`. | ✅ |
| BR-TIM-03 | All domain code gets the time from an injected `java.time.Clock`. A direct `now()` call in `domain/` fails the build (ArchUnit). | ✅ |

## 12. Error codes

Every business failure returns an RFC 9457 `ProblemDetail` (`application/problem+json`) with a stable `code` property,
plus `type` (`urn:nutricook:problem:<code>`), `title`, `detail`, `instance` and `timestamp`.
Exception messages are never returned to clients.

| Code | HTTP | Raised by |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean validation, malformed JSON, wrong parameter types, BR-PRF-03/04, BR-PRG-02/03, BR-CAT-02 (`errors[]` lists the fields). Other malformed requests (405, 415) keep their HTTP status and use this code |
| `UNAUTHENTICATED` | 401 | Missing or invalid JWT, bad credentials |
| `FORBIDDEN` | 403 | Ownership or role check |
| `NOT_FOUND` | 404 | Unknown resource |
| `EMAIL_TAKEN` | 409 | Registration |
| `PLAN_READ_ONLY` | 409 | BR-LIF-01, BR-LIF-04 |
| `PROFILE_INCOMPLETE` | 422 | BR-PRF-01 (`missingFields[]`) |
| `PROFILE_INELIGIBLE` | 422 | BR-PRF-02 |
| `PLAN_INFEASIBLE` | 422 | BR-SAF-04, BR-CMP-05 (`constraint`) |
| `SWAP_REJECTED` | 422 | BR-AI-02/03, BR-SAF-05 (`reason`) |
| `GENERATION_LIMIT` | 429 | BR-LIF-06 |
| `RATE_LIMITED` | 429 | BR-AI-07 |
| `AI_UNAVAILABLE` | 503 | BR-AI-05 (chat only) |
| `INTERNAL` | 500 | Unexpected failure. Generic message plus a `reference` id that matches the server log; nothing about the cause is returned |

## 13. Configurable constants

Domain tunables (`app.nutrition.*`, `app.tracking.*`) bind to the self-validating domain records
`NutritionRules` and `TrackingRules`. Operational limits (`app.limits.*`) bind to
`LimitsProperties`. Out-of-range values stop the application at startup. Changing a value means
changing this table first.

| Key | Default | Allowed | Rule |
|---|---|---|---|
| `app.nutrition.goal-adjustment.lose` / `.gain` | -500 / +300 | [-1000, 0] / [0, 1000] | BR-NRG-03 |
| `app.nutrition.min-kcal.male` / `.other` | 1500 / 1200 | >= 1000 | BR-NRG-04 |
| `app.nutrition.protein-per-kg.{lose,gain,maintain,improve}` | 1.8 / 2.0 / 1.2 / 1.0 | [0.8, 2.2] | BR-MAC-02 |
| `app.nutrition.protein-max-per-kg` | 2.2 | [1, 3] | BR-MAC-02 |
| `app.nutrition.protein-max-energy-share` | 0.35 | (0.10, 0.40] | BR-MAC-02 |
| `app.nutrition.fat-energy-share` | 0.30 | [0.20, 0.35] (AMDR) | BR-MAC-03 |
| `app.nutrition.fat-min-per-kg` | 0.6 | (0, 1.5] | BR-MAC-03 |
| `app.nutrition.fiber-per-1000-kcal` | 14 | (0, 30] | BR-MAC-05 |
| `app.nutrition.slot-shares.<SLOT>` | 0.25/0.10/0.30/0.10/0.25 | all 5 slots, > 0, sum = 1 | BR-CMP-01 |
| `app.nutrition.tolerance.day-ok` / `.day-max` / `.meal` | 0.05 / 0.10 / 0.15 | 0 < ok <= max < 0.5 | BR-CMP-05 |
| `app.nutrition.composer-max-attempts` | 50 | >= 1 | BR-CMP-05 |
| `app.tracking.backfill-days` | 30 | [0, 365] | BR-PRG-02 |
| `app.tracking.hydration-day-ml` | 2000 | [500, 6000] | BR-ACH-04 |
| `app.limits.generations-per-day` | 10 | [1, 1000] | BR-LIF-06 |
| `app.limits.chat-per-hour` | 30 | [1, 1000] | BR-AI-07 |
| `app.limits.ai-timeout` | 10s | required | BR-AI-05 |

---|---|---|
| `app.nutrition.goal-adjustment.lose` / `.gain` | −500 / +300 | BR-NRG-03 |
| `app.nutrition.min-kcal.male` / `.other` | 1500 / 1200 | BR-NRG-04 |
| `app.nutrition.protein-per-kg.{lose,gain,maintain,improve}` | 1.8 / 2.0 / 1.2 / 1.0 | BR-MAC-02 |
| `app.nutrition.fat-energy-share` | 0.30 | BR-MAC-03 |
| `app.nutrition.slot-shares` | 25/10/30/10/25 | BR-CMP-01 |
| `app.nutrition.tolerance.day-ok` / `.day-max` / `.meal` | 0.05 / 0.10 / 0.15 | BR-CMP-05 |
| `app.limits.generations-per-day` | 10 | BR-LIF-06 |
| `app.limits.chat-per-hour` | 30 | BR-AI-07 |
| `app.limits.ai-timeout` | 10s | BR-AI-05 |
| `app.progress.backfill-days` | 30 | BR-PRG-02 |
| `app.achievements.hydration-ml` | 2000 | BR-ACH-04 |

---

## Amendment log

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-10-10 | Initial catalogue, derived from a code audit at `39679a0` |
| 1.1.0 | 2026-10-10 | BR-SAF-09/10 added; BR-NRG-05 never rounds below the floor; BR-PRF-01 treats out-of-range stored values as missing; §12 adds `INTERNAL`; §13 lists allowed ranges. US1 rules marked ✅ |
| 1.2.0 | 2026-10-11 | BR-CAT-02 fiber-aware energy check with a 10 kcal floor; BR-SAF-02 adds shellfish/egg/soy/fish/sesame allergies; BR-SAF-10 spells out the fail-closed flag policy; BR-CAT-01 names the CSV + generated migration. US2 rules marked ✅ |
