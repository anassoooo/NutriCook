---
description: "Executable task list for 001-reliable-core"
---

# Tasks: Reliable Core

**Input**: [spec.md](spec.md), [plan.md](plan.md), [business-rules.md](../../docs/domain/business-rules.md)
**Tests**: Mandatory (Constitution V). In every story, the test tasks come first and must **fail** before implementation starts.

## Conventions

- `[P]`: can run in parallel with other `[P]` tasks in the same phase (different files, no dependency).
- `[USn]`: user story from spec.md.
- **Rules**: the BR IDs the task implements. Its tests must be named `BR_<AREA>_<NN>_…`.
- **Done when**: an objective, checkable exit condition. A task is not done until it holds **and** `./mvnw verify` / `npm test` are green.
- Paths: `B/` = `backend/src/main/java/com/nutricook/`, `BT/` = `backend/src/test/java/com/nutricook/`,
  `BR/` = `backend/src/main/resources/`, `F/` = `frontend/src/`.
- Commit per task: `type(scope): summary [Tnnn]`, e.g. `fix(safety): remove unsafe fallback [T041]`.

---

## Phase 1: Reproducible repository (M0)

**Goal**: anyone can clone, run and test the project. **Blocks everything.**

- [x] **T001** Fix `.gitignore`: anchor the Python-section rules to the repo root (`/lib/`, `/build/`, `/dist/`, `/var/`, …), remove `.specify/` from the ignores, and add LaTeX/compile artifacts (`docs/*.aux`, `*.lof`, `*.lot`, `*.out`, `*.toc`, `docs/compile_*.txt`). Then `git add frontend/src/lib/api.ts .specify/`.
  **Done when:** `git check-ignore frontend/src/lib/api.ts` prints nothing, and a fresh `git clone` + `npm ci && npm run build` in `frontend/` succeeds.
- [x] **T002** [P] Delete the stray root file `nul` and `docs/compile_*.txt`. Keep `docs/rapport_NutriCook.tex` and `.pdf` (source + output) but not the LaTeX intermediates.
  **Done when:** `git status` shows no junk files.
- [x] **T003** [P] Add the Unix Maven wrapper (`backend/mvnw`, `.mvn/wrapper/maven-wrapper.properties`) via `mvn -N wrapper:wrapper`.
  **Done when:** `./mvnw -v` works in Git Bash and Linux CI.
- [x] **T004** [P] Create root `.env.example` with `GROQ_API_KEY`, `GROQ_MODEL`, `JWT_SECRET` (and a comment: `openssl rand -base64 48`), `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE`, `DEMO_ADMIN_EMAIL`, `DEMO_ADMIN_PASSWORD`. Placeholder values only. *(`CORS_ALLOWED_ORIGINS` and `DEMO_ADMIN_*` are added by T079, when the code first reads them. `JWT_SECRET` holds a valid Base64 dev-only value so that `cp .env.example .env` works as-is.)*
  **Done when:** every `${…}` placeholder in `BR/application.yml` has an entry.
- [x] **T005** [P] `backend/Dockerfile`: multi-stage build (`eclipse-temurin:21-jdk` build → `21-jre` run), layered jar, non-root user, `HEALTHCHECK` on `/actuator/health`.
  **Done when:** `docker build backend` succeeds and the container reports healthy. *(Measured 425 MB; reducing it with a jlink custom JRE is optional future work.)*
- [x] **T006** [P] `frontend/Dockerfile` (node 24 build → `nginx:alpine`; Node 20's npm 10 rejects the npm 11 lockfile) + `frontend/nginx.conf` (SPA fallback, `/api` → `backend:8080`).
  **Done when:** `docker build frontend` succeeds.
- [x] **T007** Root `docker-compose.yml`: `db` (postgres:16-alpine, volume, `pg_isready` healthcheck), `backend` (depends on a healthy db, reads `.env`), `frontend` (port 8081). The DB is published on host port `${DB_HOST_PORT:-5433}` so it doesn't clash with a local PostgreSQL. Phase 1 uses a temporary `docker` Spring profile with `ddl-auto: update`; T011 replaces that with Flyway. Depends on T004–T006.
  **Done when:** `cp .env.example .env && docker compose up` → http://localhost:8081 shows the landing page and registration works.
- [x] **T008** [P] Frontend tooling: add ESLint (typescript-eslint, react-hooks) + Prettier configs and the scripts `typecheck` (`tsc --noEmit`), `lint`, `format`. Fix the errors that come up.
  **Done when:** `npm run typecheck && npm run lint` exit 0.
- [x] **T009** `.github/workflows/ci.yml`: a `backend` job (Temurin 21, Maven cache, `./mvnw -B verify`; Docker available for Testcontainers) and a `frontend` job (Node 24: `npm ci`, typecheck, lint, test, build) and a `docker` job (`docker compose build`). The backend job also runs `fmt-maven-plugin:check`. Add the badge to `README.md`.
  **Done when:** CI runs green on a pushed branch.

**Checkpoint M0**: fresh clone → working app + green CI.

---

## Phase 2: Foundation (M1)

**Goal**: the infrastructure every story needs. **No story work before this phase is complete.**

- [x] **T010** `backend/pom.xml`: add `flyway-core`, `flyway-database-postgresql`, `spring-boot-testcontainers`, `testcontainers:postgresql`, `testcontainers:junit-jupiter`, `net.jqwik:jqwik`, `com.tngtech.archunit:archunit-junit5`, `com.bucket4j:bucket4j_jdk17-core`, and the `jacoco-maven-plugin` (report only for now). Remove `h2`.
  **Done when:** `./mvnw dependency:tree` resolves and `verify` still passes (excluding the H2-based tests that T012 migrates).
  *As built: Bucket4j is `com.bucket4j:bucket4j-core:8.10.1` (the `_jdk17` artifact has no 8.10.1). Testcontainers is pinned to 1.21.4, because the 1.19.8 managed by Boot 3.3 is rejected by Docker Engine 29. Surefire also runs `*IT` classes.*
- [x] **T011** Flyway baseline `BR/db/migration/V1__baseline.sql`: generate it from the current entities with Hibernate schema export (PostgreSQL dialect) and review it by hand (constraint names, `uq_user_progress_date`). In `application.yml`, set `ddl-auto: validate` for **all** profiles and point `local` at `jdbc:postgresql://localhost:5432/nutricook`.
  **Done when:** the app starts against an empty compose Postgres and Flyway reports V1 applied with validate passing.
- [x] **T012** Test infrastructure: `BT/support/AbstractIntegrationTest.java` (a single shared `PostgreSQLContainer`, `@ServiceConnection`, `@SpringBootTest`, `@AutoConfigureMockMvc`) and `BT/support/StubChatModel.java` (scriptable responses, latency, exceptions). Migrate `NutriCookApplicationTests` and `AuthControllerTest` onto it. Delete `backend/src/test/resources/application-test.yml`.
  **Done when:** all existing tests pass on Postgres via Testcontainers and none references H2.
  *As built: `application-test.yml` is kept, trimmed to its non-datasource settings (JWT test key, dummy AI key).*
- [x] **T013** Error model: `B/exception/ErrorCode.java` (all of §12 with HTTP status), `B/domain/DomainException.java` (code + `Map<String,Object> details`, no Spring imports), and `B/exception/ProblemDetailHandler.java` (replaces `GlobalExceptionHandler`). Map validation errors to `errors[]`; map enum/JSON parse errors to `VALIDATION_FAILED` with the allowed values; send unknown exceptions to `INTERNAL` (logged with a correlation id, generic message).
  **Rules:** BR-PRF-04, §12. **Tests:** `BT/exception/ProblemDetailHandlerTest` covers one case per code, plus `BR_PRF_04_unknownEnumIs400`.
  **Done when:** no response body anywhere contains `ex.getMessage()` from an unexpected exception (grep `"Unexpected error"` → 0).
  *As built: `ErrorCode` lives in `B/domain/` with no HTTP knowledge, and the status mapping is an exhaustive switch in `ProblemDetailHandler`. Added code `INTERNAL` (500, with a `reference` id). `ProblemDetailSecurityHandlers` makes filter-level failures 401/403 ProblemDetails; they used to be an empty 403, and `@PreAuthorize` denials used to be a 500.*
- [x] **T014** [P] `BT/architecture/ArchitectureTest.java` with these ArchUnit rules:
  - `domain..` must not depend on `org.springframework..`, `jakarta.persistence..`, `..entity..`, `..repository..`, `..ai..`
  - no `LocalDate.now()`, `LocalDateTime.now()`, `Instant.now()` or `new Random()` inside `domain..`
  - controllers must not access repositories
  - name the no-`now()` rule `BR_TIM_03_noNowInDomain`
  **Done when:** the test runs in `verify` (domain rules pass vacuously until the domain exists).
  *As built: also `BR_CMP_07_noUnseededRandomnessInDomain`. `AchievementController` and `ReferenceController` now use services. A planted violation was confirmed to fail the build.*
- [x] **T015** [P] Configuration: `B/config/NutritionProperties.java` and `B/config/LimitsProperties.java` (`@ConfigurationProperties` records, `@Validated`, with every key from catalogue §13 and its default in `application.yml`). `B/config/DomainConfig.java` exposes a `Clock` bean (`Clock.systemUTC()`). The startup validator asserts the slot shares sum to 1.0.
  **Rules:** BR-CMP-01. **Test:** `BR_CMP_01_slotSharesMustSumToOne`: a context with shares summing to 0.9 fails to start.
  **Done when:** the app fails fast on an invalid config.
  *As built: the domain tunables are framework-free records `B/domain/rules/NutritionRules` and `TrackingRules` that validate in their constructors, bound by `DomainConfig` via `Binder`. Only `LimitsProperties` is a Spring `@ConfigurationProperties` class. Keys `app.progress.*` / `app.achievements.*` became `app.tracking.*` (catalogue §13 updated).*
- [x] **T016** [P] Traceability: `scripts/check-br-coverage.sh` and `.ps1`. They extract every `BR-[A-Z]+-[0-9]+` ID from `docs/domain/business-rules.md` (skipping rows marked RETIRED), search `backend/src/test` and `frontend/src` for `BR_<AREA>_<NN>`, and print the covered/missing table. A `--enforce` flag makes a missing ID exit 1. Add a CI step **without** `--enforce` (T085 switches it on).
  **Done when:** the script lists all 76 IDs with their current status.
- [x] **T017** [P] Domain model value objects in `B/domain/model/`: `Sex`, `ActivityLevel`, `Goal`, `NutritionProfile`, `Nutrients` (+ `plus`, `scale`, `sum`, rounding per BR-CMP-09), `Targets`, `Allergen` (EU 14), `DietFlag`, `FoodRole`, `FoodState`, `Slot`, `FoodView`, `Component`, `MealDraft`, `PlanDraft`, `Warning`. All are immutable records or enums.
  **Tests:** `BT/domain/model/NutrientsTest` covers `BR_CMP_09_componentNutritionScalesPer100g` and the rounding rules.
  **Done when:** compiles with zero Spring/JPA imports (ArchUnit green).
- [x] **T018** [P] Frontend error handling: `F/lib/errors.ts` maps every §12 `code` to a user-facing message. The axios response interceptor in `F/lib/api.ts` normalizes `ProblemDetail` into `ApiError {code, message, details}`. Add a `useApiError` hook and a toast.
  **Test:** `F/__tests__/errors.test.ts`: every code has a message, and an unknown code falls back to a generic message.
  **Done when:** no component renders `err.response.data.message` directly.
  *As built: no global toast. Pages keep their inline error areas, fed by `toApiError()` / `useApiError`. Added `ErrorCodeSyncTest` (backend) so the frontend can't miss a code. The 401 interceptor no longer reloads `/auth` after a failed login.*

**Checkpoint M1**: Postgres everywhere, migrations, error contract, domain skeleton and rule traceability in place.

---

## Phase 3: US1, Correct personal targets (P1) 🎯 M2

**Goal**: exact, single-source targets. **Independent test**: reference cases A–D through `GET /targets`.

### Tests first

- [x] **T019** [P] [US1] `BT/domain/energy/EnergyCalculatorTest.java`: `BR_NRG_01` (one test per sex, including OTHER = −78), `BR_NRG_02` (all 5 factors), `BR_NRG_03`, `BR_NRG_04` (male floor 1500; floor = BMR when BMR is higher), `BR_NRG_05` (rounding to 10), and `BR_NRG_ReferenceCases` (a `@ParameterizedTest` over A–D with exact BMR, TDEE and target).
- [x] **T020** [P] [US1] `BT/domain/energy/MacroCalculatorTest.java`: `BR_MAC_01_atwaterFactors`, `BR_MAC_02_proteinPerKgByGoal`, `BR_MAC_02_proteinCapped`, `BR_MAC_03_fatShareWithFloor`, `BR_MAC_04_carbsAreRemainder`, `BR_MAC_05_fiberPer1000kcal`, `BR_MAC_06_roundOnceAtEnd`, plus cap cases (a high-perKg light user is capped at 35% of energy; the fat floor at 0.6 g/kg kicks in at low targets). Exact values for reference cases A–D.
- [x] **T021** [P] [US1] `BT/domain/energy/AgeAndEligibilityTest.java`: `BR_TIM_02` (birthday tomorrow, birthday today, 29 Feb birthday in a non-leap year), `BR_PRF_01` (each missing field is reported, several at once), `BR_PRF_02` (17 y 364 d → ineligible; 18 → eligible; 101 → ineligible), and `BR_NRG_06` (same input with a fixed Clock → same output).

### Implementation

- [x] **T022** [US1] `B/domain/energy/EnergyCalculator.java`: Mifflin-St Jeor, activity factors, goal adjustment, floor and rounding, all reading `NutritionProperties`. Pure.
  **Rules:** BR-NRG-01..06. **Done when:** T019 is green.
- [x] **T023** [US1] `B/domain/energy/MacroCalculator.java`.
  **Rules:** BR-MAC-01..06. **Done when:** T020 is green.
- [x] **T024** [US1] `B/domain/energy/TargetService.java`: `Targets compute(NutritionProfile, LocalDate today)`. It checks completeness (`PROFILE_INCOMPLETE` + `missingFields`), then eligibility (`PROFILE_INELIGIBLE`), then energy and macros. `Targets` includes `bmr`, `tdee`, `floorApplied`.
  **Rules:** BR-PRF-01/02, BR-TIM-02. **Done when:** T021 is green.
  *As built: the domain class is named `TargetCalculator` (`B/domain/energy/`), and the service facade is `B/service/TargetService`. Reference cases live in `TargetCalculatorTest`, and `TargetPropertiesTest` adds jqwik properties (carbs never negative, macros sum to the target, never below the floor, protein within its caps). BR-NRG-05 was amended so rounding never drops below the floor.*
- [x] **T025** [US1] `BR/db/migration/V2__profile_timezone.sql` (`timezone VARCHAR(64) NOT NULL DEFAULT 'UTC'`). Add `UserProfile.timezone`, and a `B/service/UserClock.java` exposing `today(User)` = `LocalDate.now(clock.withZone(zone))`.
  **Rules:** BR-TIM-01. **Test:** `BR_TIM_01_todayUsesUserZone`: at 23:30 UTC, a user in Africa/Tunis (UTC+1) is already on the next day.
- [x] **T026** [US1] Delete `NutritionService.computeTDEE`. Rewrite `NutritionServiceTest` around the new calculators (keep its still-valid cases, renamed by BR ID). Update callers.
  **Done when:** `grep -r computeTDEE backend/src` → 0.
  *As built: a plan for an incomplete profile now fails with 422 `PROFILE_INCOMPLETE` instead of silently using 2000 kcal.*
- [x] **T027** [US1] `GET /api/users/{userId}/targets` in `B/controller/ProfileController.java` → `TargetsResponse`. The profile GET embeds `targets` (null with `targetsError` code when the profile is incomplete).
  **Rules:** BR-PRF-08. **Test:** `BT/controller/TargetsIT.java` runs reference cases A–D end-to-end, `BR_PRF_08_profileEmbedsTargets`, plus 422 for incomplete profiles and 403 for someone else's userId.
- [x] **T028** [US1] `ProfileRequest`: switch to partial-update semantics (no `@NotNull`); set bounds weight 30–300 and height 120–230; validate enums with `@ValidEnum`; accept `timezone` (a valid `ZoneId`).
  **Rules:** BR-PRF-03/04/05. **Tests:** `BR_PRF_03_*`, `BR_PRF_05_omittedFieldsUnchanged`.
  *As built: no `@ValidEnum` annotation. Enum values are checked by `Enums.parse` (400 with `allowedValues`, from T013). The timezone is validated in `UserProfileService`. The entity-level weight/height bounds were removed: validation lives at the API boundary, and the domain treats out-of-range stored values as missing.*
- [x] **T029** [P] [US1] Frontend: `F/pages/OnboardingPage.tsx` sends `Intl.DateTimeFormat().resolvedOptions().timeZone`. Add `F/hooks/useTargets.ts`. In `F/pages/DashboardPage.tsx` (current lines ~86–89, 140, 300–304, 340–343) and `F/pages/ProgressPage.tsx`, take every `max`/denominator from targets.
  **Test:** `F/__tests__/DashboardTargets.test.tsx` (MSW) checks the rings use the API values, and `BR_MAC_07_sugarNotLabelledAdded` checks there's no fixed sugar max or "added sugar" label.
  **Done when:** the CI grep guard `grep -rnE "max=\{[0-9]+\}|/ ?(2200|2500)\b" frontend/src/pages` → 0 (add this step to `ci.yml`).
  *As built: the guard also caught `DietPlanPage`, which is fixed. The test uses mocked API calls (`vi.hoisted`) instead of MSW. The profile embeds targets, and `useTargets` serves other pages. Per-meal rings use the API's per-slot targets. Added an `Other` gender option, since the forms used to default silently to Male. The `ProgressPage` had no hard-coded targets.*

**Checkpoint US1**: targets are exact, explainable (bmr/tdee shown), and single-source.

---

## Phase 4: US2, Plans are always safe (P1) 🎯 M2

**Goal**: no ineligible food can reach a user through any path. **Independent test**: the T036 property test.

### Data first (safety depends on it)

- [x] **T030** [US2] `BR/db/migration/V3__food_attributes.sql`:
  - `food_items` gets `role`, `state`, `portion_min_g`, `portion_max_g`, `portion_step_g`, `default_portion_g`, `source`, `active`
  - new tables `food_allergens(food_id, allergen)`, `food_diet_flags`, `food_suitable_slots`
  - `dietary_restrictions` gets `required_diet_flag`; new table `restriction_excluded_allergens`
  - `health_conditions`: rename `sugar_limit_g` → `sugar_limit_per_meal_g` and `fat_limit_g` → `fat_limit_per_meal_g`; add `sodium_limit_per_day_mg`; new table `condition_implied_allergens`
  - `allergens_known BOOLEAN NOT NULL DEFAULT FALSE` on foods (BR-SAF-03)

  Update the entities `FoodItem`, `DietaryRestriction` and `HealthCondition` to match (`@ElementCollection` enum sets).
  **Done when:** Flyway validate passes and the entities map with no Hibernate warnings.
  *As built: the entities use the domain enums directly (entity → domain is allowed), with `@Fetch(SUBSELECT)` so a catalog load costs one query per collection. V3 sets defaults for existing rows and deactivates them. Unverified legacy rows only come back if the catalog re-sources them by name.*
- [x] **T031** [US2] Catalog re-sourcing:
  - Write `BR/db/catalog/foods.csv` with columns name, fdc_id, state, role, kcal, protein, carbs, fat, sugar, fiber, sodium_mg, allergens, diet_flags, portion min/max/step/default, slots.
  - Write the generator `scripts/gen-food-seed.(sh|py)` → `BR/db/migration/V4__reference_data.sql`.
  - Data fixes: Brown Rice (cooked ≈ 112 kcal); legumes as **cooked** (Black Beans ≈ 132, Chickpeas ≈ 164, Kidney Beans ≈ 127); add Olive Oil (role FAT) and `Chicken Breast (halal)` / `Beef mince (halal)`.
  - Allergens: Peanut Butter = PEANUTS; Seitan, wheat bread, pastas, couscous = GLUTEN; Oatmeal = GLUTEN (cross-contamination, fail closed); tofu, tempeh, edamame = SOY; fish = FISH; shrimp = CRUSTACEANS; dairy = MILK; eggs = EGGS; tree nuts = TREE_NUTS; chia/flax/sunflower = none (role FAT).
  - Diet flags per BR-SAF-10.
  - Restriction mappings and condition limits/implications follow BR-SAF-02/06 and BR-PRF-07.

  `DataInitializer` stops seeding foods, restrictions, conditions and achievements.
  **Rules:** BR-SAF-01/02/10, BR-PRF-07, BR-CAT-01/03. **Done when:** a fresh database has the full catalog from Flyway alone.
  *As built: foods are seeded by a generated **repeatable** migration, `R__food_catalog.sql`, which upserts by name. Editing the CSV and regenerating is the whole workflow, and CI runs `--check`. `V4__reference_data.sql` holds restrictions, conditions and achievements. The catalog has 84 foods, including Tunisian staples (couscous, bulgur, Deglet Nour dates, fava beans, white beans, sea bass, tahini, olive oil). `source` names the dataset; per-item FDC ids are not recorded. Added Shellfish, Egg, Soy, Fish and Sesame allergies. BR-CAT-02 was amended to the fiber-aware form, because the plain 4/4/9 check rejects correct vegetable data. Of the 113 legacy rows, those whose names matched the catalog were re-sourced in place; the other 49 are inactive. Verified on the Compose database.*
- [x] **T032** [P] [US2] `BT/catalog/CatalogQualityIT.java`: run over every active food in the migrated DB.
  - `BR_CAT_02_atwaterWithin15Percent`
  - `BR_CAT_02_macrosNonNegativeAndBelow100`
  - `BR_SAF_03_allFoodsHaveKnownAllergens`
  - `BR_CAT_03_stateDeclared`
  - `BR_CAT_01_requiredAttributesPresent`
  - every role has ≥ 3 vegan-eligible foods (keeps strict-vegan profiles feasible)

  **Done when:** green. Every failure names the food.
  *As built: also covers `databaseMatchesTheCatalogCsv` and `BR_CAT_04_legacyRowsWithoutASourceAreInactive`. `FoodQualityGate` was created now in `B/domain/catalog/`, so T074 reuses it.*

### Tests first

- [x] **T033** [P] [US2] `BT/domain/safety/EligibilityPolicyTest.java`, table-driven:
  - `BR_SAF_02_nutAllergyExcludesPeanutButter`
  - `BR_SAF_02_glutenFreeAllowsRiceQuinoaCorn`
  - `BR_SAF_02_glutenFreeExcludesSeitanCouscous`
  - `BR_SAF_02_halalRequiresFlag`
  - `BR_SAF_03_unknownAllergensIneligibleForAllergicUser`
  - `BR_SAF_03_unknownAllergensOkWithoutAllergies`
  - `BR_PRF_07_celiacImpliesGlutenExclusion`
  - `BR_SAF_10_shellfishNeverKosher`
  - `BR_SAF_01_eligibilityIgnoresCategoryString`
- [x] **T034** [P] [US2] `BT/domain/safety/PlanSafetyValidatorTest.java`:
  - `BR_SAF_06_diabetesSugarPerMeal` (15.0 g passes, 15.1 g fails)
  - `BR_SAF_06_cholesterolFatPerMeal`
  - `BR_SAF_06_hypertensionSodiumPerDay`
  - `BR_SAF_09_kosherNoMeatWithMilk`
  - `BR_SAF_05_validatorReportsAllViolations` (it returns a list, not just the first)
- [x] **T035** [P] [US2] `BT/domain/safety/SafetyPropertiesTest.java` (jqwik, 1,000 tries): ∀ food, ∀ constraint set: `EligibilityPolicy.allows(food)` ⇒ the validator raises no per-food violation (`BR_SAF_05_policyAndValidatorAgree`).

### Implementation

- [x] **T036** [US2] `B/domain/safety/SafetyConstraints.java`: built from restrictions + conditions, including implied exclusions. It holds `excludedAllergens`, `requiredFlags`, `perMealLimits`, `perDayLimits`, `kosher`, and `hasAllergyOrIntolerance`.
  **Rules:** BR-SAF-02, BR-PRF-07.
- [x] **T037** [US2] `B/domain/safety/EligibilityPolicy.java`.
  **Rules:** BR-SAF-01/02/03/10. **Done when:** T033 and T035 are green.
- [x] **T038** [US2] `B/domain/safety/PlanSafetyValidator.java` with `validateMeal`, `validateDay` and `validate(PlanDraft)` → `List<Violation>`.
  **Rules:** BR-SAF-05/06/09. **Done when:** T034 is green.
  *As built: the policy and validator are static, stateless classes. `Violation` names the restriction or condition it comes from, so errors can say "excluded by Nut Allergy". When conditions overlap, the strictest limit wins.*
- [x] **T039** [US2] **Safety hotfix of the current generator** (ships before US3 replaces it). In `B/service/DietPlanService.java`:
  - Build the pool with `EligibilityPolicy`, and delete the `if (foods.isEmpty()) foods = foodRepository.findAll()` fallback in favor of throwing `PLAN_INFEASIBLE`.
  - Run `PlanSafetyValidator` before `save`.
  - In `applySwap`, reject foods outside the eligible pool.
  - In `GroqAiService.chat`, send the eligible food list only.

  **Rules:** BR-SAF-04/05/07. **Test:** `BT/service/SafetyHotfixIT.java`: a nut-allergy user over 50 generations never gets Peanut Butter; a scripted AI swap to Peanut Butter is rejected; a vegan + every-restriction profile gets 422 `PLAN_INFEASIBLE` and nothing is saved; `BR_SAF_07_candidateListOnlyEligible` checks the prompt the stub receives.
  *As built: 30 generations for the nut-allergy test, plus vegan+halal, Celiac and Hypertension cases. With the old one-food generator, every-restriction profiles are still feasible, so `PLAN_INFEASIBLE` is proven by deactivating the catalog inside a rolled-back transaction. The day is validated before the previous plan is deleted. Portions are still absurd (e.g. 1.5 kg of green beans) until the US3 composer.*
- [x] **T040** [P] [US2] Disclaimer: plan and profile responses carry `disclaimer: "NOT_MEDICAL_ADVICE"` when the user has a health condition, and `F/components/MedicalDisclaimer.tsx` renders it on the dashboard and plan page.
  **Rules:** BR-SAF-08. **Tests:** `BR_SAF_08_*` in the IT and the frontend test.

**Checkpoint US2 / M2**: safety is proven by property tests. The worst production bug class is closed even before the new composer.

---

## Phase 5: US3, Realistic, balanced, deterministic plans (P1) 🎯 M3

**Goal**: real meals that hit the targets, with a proper lifecycle and AI off the critical path.

- [x] **T041** [US3] `BR/db/migration/V5__plan_lifecycle.sql`:
  - `diet_plans` gets `generation_seq INT NOT NULL DEFAULT 1`, `warnings JSONB`, `ai_status VARCHAR(16)`, `seed BIGINT`; the status CHECK adds `SUPERSEDED`
  - `CREATE UNIQUE INDEX ux_active_plan ON diet_plans(user_id, valid_for_date) WHERE status='ACTIVE'`
  - `meal_food_items` gets `role` plus snapshot columns (`kcal`, `protein_g`, `carbs_g`, `fat_g`, `sugar_g`, `fiber_g`, `sodium_mg`)

  Update the entities and the `PlanStatus` enum.
  **Done when:** validate passes.
  *As built: `PlanStatus` moved to `domain.model` so the lifecycle can use it. Warnings are stored as `jsonb` via `@JdbcTypeCode(SqlTypes.JSON)`. Also added fiber/sodium totals and meal sodium, and a quota index. The migration backfills component snapshots for existing plans and demotes duplicate ACTIVE plans before creating the unique index.*

### Tests first

- [x] **T042** [P] [US3] `BT/domain/plan/PortionSolverTest.java`: `BR_CMP_03_portionsWithinBoundsAndStep` and `BR_CMP_03_clampWhenTargetUnreachable`. Hand-computed examples (e.g. a 690 kcal breakfast from Greek yogurt, oats and banana).
- [x] **T043** [P] [US3] `BT/domain/plan/PlanComposerTest.java` (fixture catalog in `BT/domain/fixtures/TestCatalog.java`):
  - `BR_CMP_02_mainMealsHaveProteinCarbProduce`
  - `BR_CMP_02_snacksHaveOneOrTwoComponents`
  - `BR_CMP_04_slotSuitabilityRespected`
  - `BR_CMP_05_referenceProfilesWithin5Percent` (A–D)
  - `BR_CMP_05_warningBetween5And10`
  - `BR_CMP_05_infeasibleBeyond10`
  - `BR_CMP_06_proteinFoodOncePerDay`
  - `BR_CMP_07_sameSeedSamePlan`
  - `BR_CMP_07_differentSeqDifferentPlan`
  - `BR_SAF_04_emptyRolePoolIsInfeasibleNamingRole`
- [x] **T044** [P] [US3] `BT/domain/plan/PlanPropertiesTest.java` (jqwik, 1,000 tries over arbitrary profiles × restriction subsets × condition subsets × seeds):
  - `BR_SAF_04_neverViolatesOrInfeasible`
  - `BR_CMP_08_totalsEqualSumOfComponents`
  - `BR_CMP_03_allPortionsInBounds`
  - `BR_CMP_07_deterministic`
- [x] **T045** [P] [US3] `BT/domain/plan/PlanLifecycleTest.java`:
  - `BR_LIF_01_allowedTransitions`
  - `BR_LIF_01_illegalTransitionsReadOnly` (every pair from the state table)
  - `BR_LIF_03_dateWindow` (today and today+7 accepted; today−1 and today+8 rejected)
  - `BR_LIF_04_onlyActiveFuturePlansMutable`

### Implementation

- [x] **T046** [US3] `B/domain/plan/SeededRandom.java` (wraps `RandomGenerator` from `seed(userId,date,seq)`; per-slot sub-streams so changing one slot doesn't reshuffle the others) and `SlotTemplate.java`.
  **Rules:** BR-CMP-02/07.
  *As built: this is `PlanSeed` (a SplitMix64 mix plus one `SplittableRandom` stream per slot and day attempt) together with `SlotGoal`. There is no separate `SlotTemplate` class; the patterns are constants in `PlanComposer`.*
- [x] **T047** [US3] `B/domain/plan/PortionSolver.java`, per plan D2.
  **Rules:** BR-CMP-03/09. **Done when:** T042 is green.
- [x] **T048** [US3] `B/domain/plan/PlanComposer.java`, per the plan D2 pseudocode (MAX_ATTEMPTS from config).
  **Rules:** BR-CMP-01..08, BR-SAF-04. **Done when:** T043 and T044 are green.
  *As built: the first version overshot protein (164–194 g against a 96 g target) and could not feed a 1,200 kcal strict-vegan profile. It was tuned to proportional protein per slot, protein sizing net of other foods, a 50% energy cap on the protein food, scoring that penalises protein both ways, and up to 3 day attempts that keep the fewest-warnings day. Measured on 800 compositions (4 profiles × 4 constraint sets × 50 seeds): 100% within ±5% and none infeasible. `LOW_PROTEIN` appears only for strict plant-based low-calorie profiles.*
- [x] **T049** [US3] `B/domain/plan/PlanLifecycle.java` (state machine, `assertMutable(plan, today)`).
  **Rules:** BR-LIF-01/03/04. **Done when:** T045 is green.
- [x] **T050** [US3] `B/service/PlanCommandService.java`: the **only** class allowed to call `DietPlanRepository.save` (it calls `PlanSafetyValidator` first). Add an ArchUnit rule `onlyPlanCommandServiceSavesPlans` to `ArchitectureTest`.
  **Rules:** BR-SAF-05 (plan D3).
- [x] **T051** [US3] `B/service/PlanGenerationService.java`, replacing `DietPlanService.generate`. It runs in this order:
  1. quota check (BR-LIF-06, Bucket4j via `B/service/QuotaService.java`)
  *As built: there is no Bucket4j `QuotaService` for generations. The quota counts stored plans since the start of the user's day, so it is exact across restarts and instances. Bucket4j is kept for the chat rate limit (BR-AI-07).*
  2. date window (BR-LIF-03)
  3. map entities → domain
  4. targets
  5. compose
  6. supersede the previous ACTIVE plan
  7. persist via `PlanCommandService` (seq, seed, warnings)
  8. retry once on a `ux_active_plan` violation

  Delete `buildMeals`, `selectCompatibleFoods`, `isFoodCompatible` and `MEAL_CALORIE_RATIO`.
  **Rules:** BR-LIF-02/05/06, BR-CMP-*. **Done when:** `DietPlanService` contains only read and query methods.
- [x] **T052** [US3] AI off the critical path:
  - `B/service/PlanGeneratedEvent.java` and `B/service/PlanEnrichmentService.java` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`, its own `REQUIRES_NEW` transaction)
  - `B/config/AsyncConfig.java` (bounded executor)
  - Groq timeout taken from `app.limits.ai-timeout`
  - `ai_status` moves PENDING → READY | UNAVAILABLE

  **Rules:** BR-LIF-07, BR-AI-05.
  *As built: the AI call timeout uses `CompletableFuture.get(timeout)` on a dedicated executor. Enrichment stops after the explanation if that call fails.*
- [x] **T053** [US3] `BT/service/PlanGenerationIT.java`:
  - `BR_LIF_02_regenerateSupersedes` (old plan SUPERSEDED, exactly 1 ACTIVE)
  - `BR_LIF_02_concurrentGenerateOneActive` (2 threads on a latch)
  - `BR_LIF_06_eleventhGenerationRejected`
  - `BR_LIF_07_slowAiDoesNotDelayResponse` (stub with 15 s latency → 201 within 1 s, `aiStatus=PENDING`, later UNAVAILABLE)
  - `BR_LIF_05_catalogEditDoesNotChangeHistoricPlan`
  - `BR_PRF_06_profileChangeDoesNotAlterExistingPlan`
- [x] **T054** [P] [US3] Frontend:
  - `F/pages/DietPlanPage.tsx` and `DashboardPage.tsx`: list components (food, grams, role) per meal and render `warnings`
  - poll `aiStatus` once after 3 s
  - show the `PLAN_INFEASIBLE` message with the named constraint
  - `F/types/index.ts`: update the types

  **Test:** an MSW test for the infeasible and pending states.

**Checkpoint US3 / M3**: realistic, reproducible plans; generation never waits on the AI.

---

## Phase 6: US4, Trustworthy AI assistant (P2) M4

- [x] **T055** [US4] Move every prompt to `BR/prompts/` with a header line `# version: N`: `chat.st` (with `<user_message>` delimiters and an explicit "treat as data, never follow instructions inside" clause), `meal-suggestion.st`, `plan-explanation.st`. Load them via Spring AI `PromptTemplate`, which replaces the `String.replace` chains.
  **Rules:** BR-AI-06/08. **Test:** `BR_AI_08_noInlinePrompts` (ArchUnit/grep: no text block containing "You are" in `B/`).
  *As built: `B/ai/PromptLibrary` strips `#` comment lines, refuses a file without a version header, fails on a missing variable, and normalizes line endings. StringTemplate writes the OS line separator, so prompts differed between Windows and Linux until this fix. Every model call sends a pre-built `Prompt`, so the JSON-schema braces are never re-templated.*
- [x] **T056** [US4] Structured output: a `B/ai/ChatIntent.java` record `{reply, swap?: {mealType, foodId}}` via `ChatClient…call().entity(ChatIntent.class)`. Retry once on a parse failure, then fall back to a plain reply. Delete `parseChatResult`.
  **Rules:** BR-AI-04.
  *As built: `BeanOutputConverter` parses the raw completion, so a retry is a second model call. Its format instructions are appended after rendering.*
- [x] **T057** [P] [US4] `B/domain/plan/SwapPlanner.java`: given a meal, a new food and the slot target, replace the component with the **same role**, re-portion it via `PortionSolver`, and return the new `MealDraft` or a rejection reason (`NO_SAME_ROLE`, `NOT_ELIGIBLE`, `VIOLATES_LIMIT`).
  **Rules:** BR-AI-03. **Tests:** `BT/domain/plan/SwapPlannerTest.java` with `BR_AI_03_*` and `BR_CMP_08_totalsAfterSwap`.
- [x] **T058** [US4] `PlanCommandService.swap(userId, planId, mealId, foodId)`: ownership, then `PlanLifecycle.assertMutable`, then eligibility, then `SwapPlanner`, then validator, then persist. Add a manual endpoint `POST /api/users/{u}/diet-plans/{p}/meals/{m}/swap` and `GET …/meals/{m}/alternatives` (eligible foods of each role, ranked by closeness to the current kcal).
  **Rules:** BR-SAF-05, BR-LIF-04, FR-003.
  *As built: the orchestration lives in a dedicated `SwapService`, which saves through `PlanCommandService.persist`. A swap keeps the replaced component's energy, and clears the meal's AI tip because it described the old food.*
- [x] **T059** [US4] `B/service/ChatService.java`, replacing `DietPlanService.chat`:
  1. quota (BR-AI-07)
  2. build candidates (eligible foods with ids, BR-SAF-07)
  3. call the AI (no transaction, with timeout)
  4. if there's an intent, check `foodId ∈ candidates` (BR-AI-02), then call `PlanCommandService.swap`
  5. on rejection, append the reason to the reply
  6. on timeout or 429, return `AI_UNAVAILABLE`

  `ChatRequest.message`: `@Size(max=500)`.
  *As built: this is a Bucket4j token bucket per user, held in a Caffeine cache, with greedy refill as an approximation of a rolling hour. Out-of-catalog ids and lifecycle refusals don't fail the request; the reply explains them. Progress context uses the user's own day.*
- [x] **T060** [US4] `BT/service/ChatServiceIT.java` with a scripted `StubChatModel`:
  - `BR_AI_02_unknownFoodIdIgnored`
  - `BR_AI_03_swapChangesOnlySameRoleComponent`
  - `BR_AI_04_malformedTwiceFallsBackToText`
  - `BR_AI_05_timeoutReturns503Code`
  - `BR_AI_06_501CharsRejected`
  - `BR_AI_06_injectionCannotAddIneligible`
  - `BR_AI_07_31stMessageRateLimited`
  - `BR_LIF_04_swapOnArchivedPlanReadOnly`
  - `BR_SAF_06_diabetesMangoSwapRejected`
  - `BR_AI_01_numbersInAiReplyNeverPersisted`
- [x] **T061** [P] [US4] Frontend `F/components/ChatPanel.tsx`: show the rejection reason inline, show RATE_LIMITED and AI_UNAVAILABLE states, and add a 500-character counter. Add a "Swap" menu on each meal, backed by `/alternatives`.
  **Test:** MSW tests.

**Checkpoint US4**: the AI can't do anything a user couldn't do by hand, and every action goes through the same safety gate.

---

## Phase 7: US5, Progress reflects reality (P2) M4

- [x] **T062** [US5] `BR/db/migration/V6__progress_meal_completion.sql`:
  - `meals` gets `completed BOOLEAN NOT NULL DEFAULT FALSE` and `completed_at`
  - `progress_entries` gets `target_calories INT` and `calories_overridden BOOLEAN NOT NULL DEFAULT FALSE`
  - CHECK constraints mirror BR-PRG-03 (defense in depth)
  *As built: V6 also stores `diet_plans.target_calories`, because the plan's target was not recorded anywhere; earlier plans get their planned total. Legacy values outside the bounds become unknown (`null`) before the constraints are added, hand-typed calories are kept as overrides, and the old disconnected `meals_completed` counter is reset. Checked with an upgrade of the Docker database that held out-of-range rows.*
- [x] **T063** [P] [US5] `BT/domain/progress/ProgressRulesTest.java`:
  - `BR_PRG_01_mergeKeepsOmittedFields`
  - `BR_PRG_02_dateWindow` (today ok, today+1 rejected, today−30 ok, today−31 rejected)
  - `BR_PRG_03_bounds` (one case per field, edges included)
  - `BR_PRG_05_derivedCalories`
  - `BR_PRG_05_overrideWins`
  - `BR_PRG_06_targetSnapshotFromPlanElseProfile`
  *As built: 35 cases, including every bound edge, NaN weight, all invalid fields reported at once, notes clearing, reset to derived calories, and unknown (no plan) vs. zero (nothing checked).*
- [x] **T064** [US5] `B/domain/progress/ProgressRules.java` (pure): bounds, date window, derivation, merge semantics.
  **Rules:** BR-PRG-01..03/05/06. **Done when:** T063 is green.
  *As built: `domain/progress/{ProgressRules, DayLog, LogUpdate, PlanDay}`. `merge` applies the user's fields, `derive` applies what the plan decides, and `isLatestWeighIn` covers BR-PRG-04.*
- [x] **T065** [US5] `PlanCommandService.setMealCompleted(...)` + `PUT …/meals/{m}/completion`. This updates the day's progress entry (created if absent) through `ProgressService`, then triggers achievement evaluation.
  **Rules:** BR-PRG-05, FR-004.
  *As built: `ProgressService.setMealCompleted` orchestrates; `PlanCommandService.setMealCompleted` only flips the flag and refuses non-ACTIVE plans. The response carries the plan, the day's entry and new achievements. `PlanCommandService` publishes `DayPlanChangedEvent` after it saves or archives a plan, and `ProgressService` handles it in the same transaction, so regenerating, swapping or archiving keeps the day consistent without a service cycle. A checked meal cannot be swapped (`MEAL_COMPLETED`). Fixed a lost update found here: asynchronous AI text saved the whole plan row and could undo a check-off made in the meantime; it now writes only its own columns (`updateAiText`, `updateMealTip`).*
- [x] **T066** [US5] Rewrite `B/service/ProgressService.java`: user-day dates (`UserClock`), `ProgressRules`, target snapshot, and update `profile.weightKg` when the entry is the latest dated one.
  **Rules:** BR-PRG-01..06. **Test:** `BT/service/ProgressIT.java` covers `BR_PRG_04_latestWeightUpdatesProfileAndTargets` and `BR_PRG_04_backfilledWeightDoesNotOverwrite`.
  *As built: 15 scenarios in `ProgressIT`, including regeneration, archiving, the late AI text regression and the range guard on reads (ordered, at most 366 days). `mealsCompleted` is no longer accepted as input; `resetCalories` returns to derived calories.*
- [x] **T067** [P] [US5] Frontend: meal check-off toggles on the dashboard; `ProgressPage` input bounds mirror BR-PRG-03 (the server stays authoritative); show "auto" vs "manual" calories with a reset-to-auto action.
  *As built: `components/MealCheck` (an accessible checkbox), `lib/progressRules` for the bounds and local dates (the pages used the UTC date from `toISOString`), and server errors are now shown, field by field, where they used to be swallowed. `__tests__/Progress.test.tsx`.*

---

## Phase 8: US6, Fair achievements (P2) M4

- [x] **T068** [US6] `BR/db/migration/V7__achievement_types.sql`: rename `LOGIN_STREAK` → `LOGGING_STREAK`, delete `PROTEIN_GOAL` rows, rewrite descriptions to state the exact rule (e.g. "Hit within ±10% of your calorie target on 10 days in the last 30"), and add WEIGHT_MILESTONE achievements (2 kg, 5 kg). Update the `AchievementType` enum.
  *As built: the enum moved to `domain.model`, since the domain evaluates it. The milestones are "On the Move" (2 kg) and "Transformation" (5 kg). A partial index serves the first weigh-in lookup. Checked by upgrading the Docker database that held a legacy streak award and a PROTEIN_GOAL award: the first was kept, the second removed.*
- [x] **T069** [P] [US6] `BT/domain/achievement/EvaluatorsTest.java`, table-driven with synthetic histories:
  - `BR_ACH_01_streakGraceUntilEndOfDay`
  - `BR_ACH_01_gapBreaksStreak`
  - `BR_ACH_02_supersededNotCounted`
  - `BR_ACH_03_onlyWithin10PercentCounts`
  - `BR_ACH_04_hydrationRolling30`
  - `BR_ACH_05_weightMilestoneDirectionAware`
  - `BR_ACH_05_everyTypeHasEvaluator` (iterates `AchievementType.values()`)
  *As built: 34 cases. `BR_ACH_02_supersededNotCounted` only checks the pass-through here; the distinct count is a query, covered end to end by `AchievementIT.BR_ACH_02_regenerationsCountOnce`.*
- [x] **T070** [US6] `B/domain/achievement/`: the `AchievementEvaluator` interface, one class per type, `EvaluationContext` (entries for the last 365 days, distinct plan-date count, first and latest weight, goal), and `EvaluatorRegistry` (an `EnumMap` that fails at construction if a type is missing).
  **Rules:** BR-ACH-01..05.
  *As built: the context carries the history (365 days), the planned-day count, the first weight ever and the goal; the latest weight is read from the history. The registry is a bean, so a missing evaluator fails startup.*
- [x] **T071** [US6] Rewrite `B/service/AchievementService.java`: build the context with **one** progress range query plus one count query, then evaluate. Insert with `INSERT … ON CONFLICT (user_id, achievement_id) DO NOTHING` (native `@Modifying` query). The response includes `currentValue`, `threshold` and `earned` for every achievement (the frontend shows progress without computing it).
  **Rules:** BR-ACH-06/07, Principle VIII.
  *As built: the history needs three reads (range, distinct plan days, first weigh-in) because the first weight can be older than the 365-day range. `GET /users/{id}/achievements` now returns every achievement with its standing, instead of only the earned ones.*
- [x] **T072** [US6] `BT/service/AchievementIT.java`: `BR_ACH_06_concurrentAwardSingleRow` (2 threads), `BR_ACH_06_awardNeverRevoked`, and `BR_ACH_07_atMostThreeQueries` (Hibernate `Statistics`, or the datasource-proxy query counter).
  *As built: the query test is `BR_ACH_07_queryCountDoesNotGrowWithHistory`: it compares a 1-day and a 100-day history (same count) and bounds the total at 8 statements, which are the profile with its 2 eager collections, the 3 history reads, the existing awards and the catalog. 7 scenarios in all, including calorie days judged against their own target and a goal-aware weight milestone.*
- [x] **T073** [P] [US6] Frontend `F/pages/AchievementsPage.tsx`: progress bars from `currentValue/threshold` (no client computation).
  *As built: one request instead of two; the client-side requirement labels (which used the old type names) were removed, since descriptions now state the rules. Accessible progress bars. `__tests__/Achievements.test.tsx`.*

**Checkpoint M4**: the plan → eat → log → reward loop is consistent end to end.

---

## Phase 9: US7, Curated food catalog (P3) M5

- [ ] **T074** [US7] `B/domain/catalog/FoodQualityGate.java` (pure, BR-CAT-02; reused by `CatalogQualityIT` from T032) and `B/service/CatalogAdminService.java` (create, update, soft-delete; name unique, case-insensitive).
  **Rules:** BR-CAT-01/02/04.
- [ ] **T075** [US7] `B/controller/AdminFoodController.java` (`/api/admin/foods`, `@PreAuthorize("hasRole('ADMIN')")`, paginated GET with `?q=&role=&active=`) with request/response DTOs.
  **Rules:** BR-CAT-05, FR-005.
- [ ] **T076** [US7] Startup gate `B/config/CatalogStartupCheck.java`: an `ApplicationRunner` that validates every active food and refuses to start if any fails, naming them.
  **Rules:** BR-CAT-02.
- [ ] **T077** [US7] `BT/controller/AdminFoodIT.java`:
  - `BR_CAT_02_atwaterViolationRejected`
  - `BR_CAT_04_softDeleteKeepsHistory`
  - `BR_CAT_04_inactiveExcludedFromNewPlans`
  - `BR_CAT_05_nonAdminForbidden`
- [ ] **T078** [P] [US7] Frontend `F/pages/admin/FoodsPage.tsx`: a table with search and a form with allergen/flag checkboxes. The route is guarded by `role === 'ADMIN'`.

---

## Phase 10: Hardening, documentation, delivery (M5)

- [ ] **T079** Security configuration (`B/config/SecurityConfig.java`, `application.yml`):
  - CORS origins come from `CORS_ALLOWED_ORIGINS`
  - remove `/h2-console` and the `frameOptions().disable()`
  - Swagger/api-docs off in `prod`
  - `JwtSecretValidator` fails startup in `prod` if the secret is < 32 bytes or equals the dev default
  - `DataInitializer` becomes `DemoDataInitializer` with `@Profile("local")`, credentials from `DEMO_ADMIN_*`, and it never logs passwords

  **Test:** `BT/config/ProdProfileIT.java` checks the secret fail-fast and that `/swagger-ui.html` → 404 in prod.
- [ ] **T080** [P] Observability: a `B/config/CorrelationIdFilter.java` (MDC `requestId`, echoed in the `X-Request-Id` header and the ProblemDetail `instance`); structured log lines for generation (userId, seed, seq, attempts, warnings, ms) and AI calls (latency, outcome; never prompt contents). Expose Actuator `metrics` and `prometheus` to admins only.
- [ ] **T081** [P] Update `docs/diagrams/class-data.mmd` (new entities and columns) and `docs/diagrams/seq-diet.mmd` (generate → commit → async enrich; chat → intent → swap command), then re-render the PNGs.
  **Done when:** the diagrams match the V1–V7 schema.
- [ ] **T082** [P] Update `.specify/templates/plan-template.md` (Constitution Check table for I–X) and `tasks-template.md` (mandatory tests, a "Done when" line, BR references). This clears the ⚠ items in the constitution's Sync Impact Report.
- [ ] **T083** README rewrite:
  - a one-paragraph pitch
  - a GIF demo
  - a live link
  - an architecture diagram (layers + async AI)
  - a **"How correctness is guaranteed"** section (rule catalogue → tests by ID → property tests → CI gate)
  - quickstart (`docker compose up`)
  - testing commands
  - project structure
  - a link to the business rules

  **Done when:** someone who didn't write the project can run it from the README alone.
- [ ] **T084** [P] Deployment guide `docs/deploy.md` + deploy: Neon Postgres, Render (backend Docker, env vars), Vercel (frontend, `/api` rewrite to Render). Add the live URL to the README.
  **Done when:** the public URL serves a working register → onboarding → plan → chat flow.
- [ ] **T085** Turn the gates on: JaCoCo `check` (domain ≥ 90%, service ≥ 75% line) and `check-br-coverage --enforce` in CI.
  **Done when:** CI is green with both enforced (this is SC-001 and SC-005).
- [ ] **T086** Final audit: update the **Today** column of every rule in `business-rules.md` to ✅ (or RETIRED with a reason), bump the catalogue to 1.1.0, and add the amendment-log row. Then run `/speckit.analyze` for cross-artifact consistency.
  **Done when:** no ⚠️ or ❌ rows remain.

---

## Dependencies & execution order

```text
Phase 1 (M0) ──► Phase 2 (M1) ──► Phase 3 US1 ──► Phase 4 US2 ──► Phase 5 US3 ──┬─► Phase 6 US4
                                                                               ├─► Phase 7 US5 ──► Phase 8 US6
                                                                               └─► Phase 9 US7
                                                     all ──► Phase 10
```

- **US2 depends on US1** only for `NutritionProfile`/`Targets` types; T030–T035 can start once T017 is done.
- **US3 depends on US2** (composer uses `EligibilityPolicy` and `PlanSafetyValidator`).
- **US4, US5, US7 are independent of each other** after US3; **US6 depends on US5** (target snapshot, check-off events).
- Within a phase: migration → tests (fail) → domain → service → controller → frontend.

## Parallel opportunities

- Phase 1: T002–T006 and T008 in parallel.
- Phase 2: T014–T018 in parallel after T010–T013.
- US1: T019–T021 together, then T022 ∥ T023.
- US2: T032–T035 together after T031.
- US3: T042–T045 together.
- After M3: US4, US5, US7 in parallel tracks.

## Implementation strategy

1. **MVP of trust (M0 → M2)**: Phases 1–4. Ship it. The app is reproducible, the numbers are exact, and it is provably safe.
   The T039 hotfix means safety lands **before** the big composer rewrite.
2. **M3**: the composer makes plans credible, which is the biggest visible quality jump.
3. **M4**: the AI, progress and achievements loop becomes coherent.
4. **M5**: admin, hardening, deploy. The CV-ready release is tagged `v1.0.0`.

At every checkpoint: CI green, the BR coverage report attached to the PR, and the "Today" column updated for the rules touched.

## Task count

86 tasks: Setup 9 · Foundation 9 · US1 11 · US2 11 · US3 14 · US4 7 · US5 6 · US6 6 · US7 5 · Polish 8.
