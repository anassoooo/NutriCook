# Implementation Plan: Reliable Core

**Branch**: `001-reliable-core` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)
**Rules**: [business-rules.md](../../docs/domain/business-rules.md) | **Constitution**: v2.0.0

## Summary

Rebuild NutriCook's business core as a **pure, deterministic domain layer** that implements the
catalogued rules. The existing Spring services become thin orchestrators around it. Safety becomes a
single fail-closed gate that every write passes through. The AI is demoted to a proposal engine
whose intents go through the same command path as user actions. Foundations come first (reproducible
build, migrations, Postgres-based tests, error model) so every later change ships with proof.

## Technical context

**Language/Version**: Java 21, TypeScript 5
**Primary dependencies**: Spring Boot 3.3.4, Spring AI 1.0, JJWT 0.12. To add: Flyway, Bucket4j,
jqwik, Testcontainers, ArchUnit, JaCoCo, MSW (frontend)
**Storage**: PostgreSQL 16 in every environment (Compose locally, Testcontainers in tests). H2 is retired
because it can't enforce the partial unique index (D5) and hides dialect bugs.
**Testing**: JUnit 5 + AssertJ (unit), jqwik (properties), Testcontainers (integration), Vitest + MSW (UI)
**Target platform**: Linux containers (Render/Fly backend, Vercel frontend, Neon Postgres)
**Performance goals**: plan generation p95 < 300 ms excluding AI; achievement evaluation ≤ 3 queries
**Constraints**: zero cost (Groq free tier, so quotas are needed); deterministic domain
**Scale**: portfolio scale (≤ 1k users); correctness over throughput

## Constitution check

| # | Principle | How this plan complies | Status |
|---|---|---|---|
| I | Business rules are the product | Every task cites BR IDs; constants move to `NutritionProperties` | ✅ |
| II | Safety fails closed | `EligibilityPolicy` + `PlanSafetyValidator`; `findAll()` fallback removed; `PLAN_INFEASIBLE` | ✅ |
| III | AI advises, domain decides | Structured intents restricted to candidate ids; swap through `PlanCommandService`; AI after commit | ✅ |
| IV | Pure deterministic domain | `com.nutricook.domain..` has no Spring/JPA; `Clock` + seeded RNG; ArchUnit enforced | ✅ |
| V | Every rule tested by ID | `BR_*` test naming, `scripts/check-br-coverage`, jqwik, JaCoCo gates | ✅ |
| VI | Migrations own the schema | Flyway V1 baseline; `ddl-auto: validate` everywhere; seeds become migrations | ✅ |
| VII | Ownership & least privilege | ProblemDetail codes; prod CORS/H2/Swagger lockdown; JWT secret fail-fast; admin seeding local-only | ✅ |
| VIII | Frontend displays | `/targets` endpoint; remove hard-coded maxima; error-code map | ✅ |
| IX | Zero-cost reproducible | Docker Compose, `.env.example`, GitHub Actions | ✅ |
| X | Scope discipline | No new product areas; recipes/RAG deferred to their own spec | ✅ |

## Architecture

### Layering (enforced by ArchUnit, task T014)

```text
controller ──► service (application) ──► domain (pure)
     │               │      │
     │               │      └──► ai (GroqAiService: ChatClient, prompts)
     │               └──► repository ──► entity
     └──► dto
domain  ✗ must not import: org.springframework.., jakarta.persistence.., ..entity.., ..ai..
```

### Package layout (backend/src/main/java/com/nutricook/)

```text
domain/
├── model/          # value objects: NutritionProfile, Sex, Targets, Nutrients, FoodView,
│                   #   Allergen, DietFlag, FoodRole, Slot, Component, MealDraft, PlanDraft, Warning
├── energy/         # EnergyCalculator (BR-NRG), MacroCalculator (BR-MAC), TargetService facade
├── safety/         # EligibilityPolicy (BR-SAF-01..03, BR-PRF-07), PlanSafetyValidator (BR-SAF-05/06),
│                   #   Violation, SafetyConstraints (built from restrictions + conditions)
├── plan/           # PlanComposer (BR-CMP), PortionSolver, SlotTemplate, SeededRandom,
│                   #   PlanLifecycle (BR-LIF state machine), SwapPlanner (BR-AI-03)
├── progress/       # ProgressRules (BR-PRG bounds, dates, derivation)
├── achievement/    # AchievementEvaluator interface + one class per type, EvaluationContext
└── DomainException # carries ErrorCode + details (no HTTP knowledge)
config/
├── NutritionProperties, LimitsProperties   # @ConfigurationProperties records, @Validated
└── DomainConfig                            # @Bean wiring of domain classes + Clock
service/            # application services: transactions, repos, mapping entity ↔ domain
├── TargetAppService, PlanGenerationService, PlanCommandService (swap, complete, archive)
├── PlanEnrichmentService (AI, @Async after commit), ChatService, ProgressService,
├── AchievementService, CatalogAdminService, QuotaService (Bucket4j)
exception/          # ErrorCode enum, ProblemDetailHandler (DomainException → ProblemDetail)
```

### Key design decisions

**D1. Mapping entities to domain value objects.** Services load JPA entities and map them to
immutable records (`FoodView`, `NutritionProfile`). The domain returns a `PlanDraft`, which the service maps back
to entities. *Why*: keeps the domain pure and property-testable without a DB. *Cost*: some mapping
code. MapStruct is rejected because the mappings are small and explicit code is easier to explain.

**D2. Plan composition algorithm** (`PlanComposer`, BR-CMP)

```text
input: Targets t, SafetyConstraints c, List<FoodView> catalog, seed
eligible = catalog.filter(EligibilityPolicy.allows(c))               # BR-SAF-01..03
for slot in [BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER]:
    kcal_s = t.kcal × share(slot);  prot_s = t.protein × share(slot)
    template = SlotTemplate.of(slot)        # main: PROTEIN+CARB+PRODUCE(+FAT) | snack: 1–2 roles
    pools = template.roles → eligible.filter(role, suitableSlots ∋ slot, variety rules)
    if any required pool empty → throw PLAN_INFEASIBLE(role, slot)    # BR-SAF-04
    best = null
    for attempt in 1..MAX_ATTEMPTS (50):           # candidates drawn by SeededRandom(seed, slot)
        pick one food per role
        PortionSolver:
          protein.g = clamp(prot_s / protein.per_g, bounds)
          produce.g = produce.defaultPortion
          carb.g    = clamp((kcal_s − kcal(protein, produce)) / carb.kcal_per_g, bounds)
          if gap > meal tolerance and FAT role allowed: fat.g = clamp(gap / 9 …)
          round all to step                                            # BR-CMP-03
        if PlanSafetyValidator.checkMeal(meal, c) has violations → continue   # BR-SAF-06
        score = |kcal err|/kcal_s + 0.5·|prot err|/prot_s
        best = min(best, score); if score within tolerance → break
    if best == null → PLAN_INFEASIBLE(slot, last violation)
day check: if |Δkcal| > 5% → one proportional pass over CARB portions (within bounds)
           classify: ≤5% ok | ≤10% warning TARGET_DEVIATION | >10% PLAN_INFEASIBLE   # BR-CMP-05
PlanSafetyValidator.checkDay(plan, c)  (sodium/day etc.) → violation = PLAN_INFEASIBLE
return PlanDraft(meals, totals = Nutrients.sum(...), warnings)
```

This is a greedy heuristic, not an LP solver. It is good enough for a 5-slot day, explainable in a
defense, and deterministic. An LP solver (OR-Tools) is noted as future work in Complexity Tracking.

**D3. One safety gate.** `PlanSafetyValidator.validate(PlanDraft, SafetyConstraints)` is called by the
composer (to prune) **and** again by `PlanCommandService.persist(...)` just before every save. That
second call is defense in depth: no write path can skip it, and a test asserts every repository
`save(DietPlan)` call site goes through `persist` (ArchUnit rule: only `PlanCommandService` may call
`DietPlanRepository.save`).

**D4. Transactions and AI.** Generation = `@Transactional` {load, compose, supersede previous, save}
→ commit → publish `PlanGeneratedEvent` → `@TransactionalEventListener(AFTER_COMMIT) @Async`
`PlanEnrichmentService` calls Groq with a 10 s timeout and writes `aiExplanation` / meal tips in a
**new** small transaction. The API returns the plan immediately; the UI polls or refetches for the
explanation. Chat: the AI call runs outside any transaction, then `PlanCommandService.swap(...)` runs in its own
transaction.

**D5. One ACTIVE plan per user-day under concurrency.** Postgres partial unique index
`ux_active_plan ON diet_plans(user_id, valid_for_date) WHERE status = 'ACTIVE'`, plus supersede-then-insert
in one transaction. On a unique violation, retry once.

**D6. Deterministic seed.** `seed = Objects.hash(userId, date, generationSeq)`. `generationSeq` = count
of that user-day's plans + 1. It is stored on the plan so any plan can be regenerated byte-for-byte in a test.

**D7. Quotas.** Bucket4j in-memory buckets keyed by userId (generation per user-day, chat per hour).
This is acceptable for a single instance. Moving to Redis/Postgres is noted for scale-out.

**D8. Allergen data model.** `@ElementCollection` enum sets (`food_allergens`, `food_diet_flags`,
`food_suitable_slots`) instead of bitmasks: readable SQL, simple JPA, fine at this scale.

**D9. Catalog re-sourcing.** Foods are re-entered from USDA FoodData Central with an explicit `state`
(cooked values for grains and legumes, since that matches portion intuition). Each row stores `source = "FDC:<id>"`.
The Atwater gate (BR-CAT-02) runs as a migration-time test and on admin save.

## Migration plan (Flyway)

| Version | Content |
|---|---|
Version numbers follow task order, so each phase ships its own migration.

| Version | Phase | Content |
|---|---|---|
| `V1__baseline.sql` | 2 | Current schema exactly as Hibernate generates it today (Postgres dialect) |
| `V2__profile_timezone.sql` | 3 (US1) | `user_profiles.timezone` |
| `V3__food_attributes.sql` | 4 (US2) | role, state, portion bounds, source, active; allergen/flag/slot tables; restriction/condition rule columns |
| `V4__reference_data.sql` | 4 (US2) | Re-sourced foods with allergens/flags (generated from `db/catalog/foods.csv`); restriction and condition mappings; fixed achievement descriptions |
| `V5__plan_lifecycle.sql` | 5 (US3) | SUPERSEDED, generation_seq, warnings, ai_status, partial unique index, component role + snapshot columns |
| `V6__progress_meal_completion.sql` | 7 (US5) | meal completed/completed_at; progress target_calories, calories_overridden; CHECK constraints for BR-PRG-03 |
| `V7__achievement_types.sql` | 8 (US6) | LOGIN_STREAK → LOGGING_STREAK; remove PROTEIN_GOAL rows |

Existing local H2 data is disposable (it is a dev database). Production has never started
successfully (`validate` with no schema), so there is no prod data to migrate.

## Testing strategy

| Layer | Tooling | What | Gate |
|---|---|---|---|
| Domain unit | JUnit 5 + AssertJ | Every BR rule (`BR_XXX_NN_*`), reference cases A–D | 90% line |
| Domain property | jqwik | ∀ profile × restrictions × conditions: no violation, or INFEASIBLE; totals = Σ components; portions in bounds; determinism | 1,000 tries in CI |
| Architecture | ArchUnit | Domain purity, no `now()` in domain, only `PlanCommandService` saves plans | build fails |
| Service integration | `@SpringBootTest` + Testcontainers PG + stub `ChatModel` | Transactions, concurrency (D5), quotas, AI degradation, achievements query count | 75% line |
| API contract | MockMvc | ProblemDetail codes for every §12 code, ownership 403s | — |
| Data quality | JUnit over V3 seed | BR-CAT-02 Atwater, allergen completeness | build fails |
| Frontend | Vitest + MSW | Error-code messages, targets rendered from API, no hard-coded numbers (grep) | CI |
| Traceability | `scripts/check-br-coverage.(sh|ps1)` | Every non-retired BR ID has a test | CI |

## Delivery milestones

| Milestone | Tasks | Demo-able outcome |
|---|---|---|
| **M0 Reproducible** | Phase 1 | Fresh clone → `docker compose up` → app runs; CI green badge |
| **M1 Foundation** | Phase 2 | Flyway, Postgres tests, ProblemDetail, domain skeleton, BR coverage script |
| **M2 Correct & Safe** | Phases 3–4 (US1, US2) | Targets endpoint; safety property test with 1,000 cases green |
| **M3 Realistic plans** | Phase 5 (US3) | Balanced, deterministic plans; AI after commit |
| **M4 Trustworthy loop** | Phases 6–8 (US4–US6) | Safe AI swaps, meal check-off, fair achievements |
| **M5 Admin & ship** | Phases 9–10 (US7, polish) | Admin catalog, security hardening, deployed demo, README |

## Risks

| Risk | Mitigation |
|---|---|
| Re-sourcing ~70 foods with allergens is tedious and error-prone | Script a CSV → SQL generator; Atwater + allergen-completeness tests catch mistakes |
| Greedy composer can't hit tolerance for very restrictive profiles | That is correct behavior (`PLAN_INFEASIBLE`); expand catalog (tofu, tempeh, lentils are key) |
| Async enrichment complicates the UI | Response includes `aiStatus: PENDING|READY|UNAVAILABLE`; UI refetches once after 3 s |
| Groq free-tier limits during demo | Quotas + cached explanation per plan; demo works fully without AI |
| Scope creep | Constitution X: recipes/RAG stay in a later spec |

## Complexity tracking

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| Separate domain layer + mapping | Pure, property-testable rules (Principle IV) | Logic inside JPA entities can't be property-tested without a DB |
| Async AI enrichment | Keeps DB transactions short; generation never blocked (BR-LIF-07) | Synchronous calls hold a transaction for up to 6 × 10 s |
| Greedy composer instead of LP | Explainable, no native deps, deterministic | OR-Tools adds native binaries; kept as future work |
