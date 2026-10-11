# NutriCook — Architecture and Class Diagram

**Version**: 2.0.0 | **Date**: 2026-10-11

> How the backend is layered and where each business rule lives. Tables are in
> [`data-model.md`](data-model.md), runtime flows in [`sequence-diagrams.md`](sequence-diagrams.md)
> and the rules themselves in [`../domain/business-rules.md`](../domain/business-rules.md).

---

## 1. Layers

```mermaid
flowchart TB
    FE["Frontend (React 18, Vite, TypeScript)<br/>displays; never computes business numbers"]
    subgraph Backend["Backend (Spring Boot 3.3, Java 21)"]
        direction TB
        WEB["Web: controllers, DTOs, ProblemDetailHandler<br/>validation, stable error codes"]
        SEC["Security: JWT filter, owner checks,<br/>ADMIN for /api/admin and /actuator"]
        APP["Application services<br/>transactions, orchestration, persistence"]
        DOM["Domain (pure Java)<br/>energy, safety, composer, swaps, lifecycle,<br/>progress, achievements, catalog gate"]
        AI["AI adapter: GroqAiService, PromptLibrary<br/>versioned prompts, timeouts"]
    end
    DB[("PostgreSQL 16<br/>schema owned by Flyway")]
    LLM["Groq LLM<br/>(llama-3.3-70b)"]
    OSM["OpenStreetMap<br/>Overpass API"]

    FE -->|REST, JSON, JWT| SEC --> WEB --> APP
    APP --> DOM
    APP --> DB
    APP --> AI --> LLM
    APP --> OSM
```

The arrows are the only allowed dependencies. ArchUnit (`ArchitectureTest`) fails the build if:

- `com.nutricook.domain..` depends on Spring, JPA, the web layer or the system clock
  (Constitution IV, BR-TIM-03);
- any class other than `PlanCommandService` saves a plan (BR-SAF-05: nothing unsafe is persisted).

---

## 2. The domain core

Everything that decides something lives here, as plain Java with injected time and tunables.
It is tested without Spring: unit, table-driven and property-based (jqwik) tests.

```mermaid
classDiagram
    direction LR
    class TargetCalculator {
        +compute(NutritionProfile, LocalDate today) Targets
        +perSlot(Targets) Map
    }
    class EnergyCalculator {
        +target(...) EnergyTarget
    }
    class MacroCalculator
    class EligibilityPolicy {
        +allows(FoodView, SafetyConstraints) boolean
    }
    class PlanSafetyValidator {
        +validate(PlanDraft, SafetyConstraints) List~Violation~
    }
    class PlanComposer {
        +compose(Targets, constraints, foods, date, seed) PlanDraft
    }
    class PortionSolver
    class PlanSeed {
        +of(userId, date, seq) long
    }
    class SwapPlanner {
        +swap(PlanDraft, Slot, FoodView, constraints) Result
        +alternatives(...) List~Component~
    }
    class PlanLifecycle {
        +transition(from, to) PlanStatus
        +assertMutable(status, planDate, today)
    }
    class ProgressRules {
        +assertLoggable(date, today, backfill)
        +assertWithinBounds(LogUpdate)
        +merge(DayLog, LogUpdate) DayLog
        +derive(DayLog, PlanDay, profileTarget) DayLog
    }
    class EvaluatorRegistry {
        +currentValue(type, ctx) double
        +earned(type, threshold, ctx) boolean
    }
    class AchievementEvaluator {
        <<interface>>
        +currentValue(EvaluationContext) double
    }
    class FoodQualityGate {
        +problems(Nutrients) List~String~
        +labelProblems(allergens, flags, meat) List~String~
    }
    class NutritionRules {
        <<record>>
    }
    class TrackingRules {
        <<record>>
    }

    TargetCalculator --> EnergyCalculator
    TargetCalculator --> MacroCalculator
    TargetCalculator --> NutritionRules
    PlanComposer --> PortionSolver
    PlanComposer --> EligibilityPolicy
    PlanComposer --> PlanSafetyValidator
    PlanComposer --> NutritionRules
    SwapPlanner --> EligibilityPolicy
    SwapPlanner --> PlanSafetyValidator
    EvaluatorRegistry --> AchievementEvaluator
    EvaluatorRegistry --> TrackingRules
```

| Package | Rules | Key idea |
|---|---|---|
| `domain.energy` | BR-NRG, BR-MAC | Mifflin-St Jeor, goal adjustment, a floor that rounding never breaks |
| `domain.safety` | BR-SAF | Allergens, diet flags and condition limits; unknown data fails closed |
| `domain.plan` | BR-CMP, BR-LIF, BR-AI-03 | Seeded composer, portion solver, role-based swaps, lifecycle state machine |
| `domain.progress` | BR-PRG | Date window, bounds, merge, derived calories, target snapshot |
| `domain.achievement` | BR-ACH | One evaluator per type, registry that fails startup when one is missing |
| `domain.catalog` | BR-CAT-02 | Energy matches macros, labels match the food |
| `domain.rules` | §13 | Every tunable (`app.nutrition.*`, `app.tracking.*`), range-checked at startup |

---

## 3. Application services

```mermaid
classDiagram
    direction TB
    class PlanGenerationService {
        +generate(userId, date) DietPlan
    }
    class PlanCommandService {
        <<SoleWriter>>
        +persist(plan, constraints)
        +supersede(plan)
        +archive(plan)
        +setMealCompleted(plan, meal, completed)
        +saveAiText(planId, text, tips, status)
    }
    class PlanEnrichmentService {
        <<Async>>
    }
    class SwapService
    class ChatService
    class GroqAiService
    class ProgressService {
        +log(userId, request)
        +setMealCompleted(userId, planId, mealId, completed)
        +onDayPlanChanged(event)
    }
    class AchievementService {
        +checkAndAward(userId)
        +standingsFor(userId)
    }
    class CatalogAdminService
    class SafetyService
    class TargetService
    class UserClock

    PlanGenerationService --> PlanCommandService
    PlanGenerationService --> TargetService
    PlanGenerationService --> SafetyService
    PlanGenerationService ..> PlanEnrichmentService : PlanGeneratedEvent
    PlanEnrichmentService --> GroqAiService
    PlanEnrichmentService --> PlanCommandService
    SwapService --> PlanCommandService
    ChatService --> GroqAiService
    ChatService --> SwapService
    ProgressService --> PlanCommandService
    PlanCommandService ..> ProgressService : DayPlanChangedEvent
    ProgressService --> TargetService
    TargetService --> UserClock
```

- **One writer.** `PlanCommandService` (`<<SoleWriter>>`) is the only class that saves plans.
- **Events instead of cycles.** `PlanCommandService` publishes `DayPlanChangedEvent` after it saves
  or archives a plan; `ProgressService` handles it in the same transaction, so a day's entry always
  follows its plan. `PlanGeneratedEvent` is handled only after commit, so the AI never delays or
  breaks plan creation (BR-LIF-07).
- **Lost updates avoided.** The AI text arrives asynchronously, so it is written with targeted
  `UPDATE`s of its own columns, never by saving the whole plan row.

---

## 4. Web and security

| Area | Classes | Notes |
|---|---|---|
| Endpoints | `AuthController`, `ProfileController`, `TargetController`, `DietController`, `ProgressController`, `AchievementController`, `LocationController`, `ReferenceController`, `AdminFoodController` | Every `/api/users/{userId}/**` endpoint checks ownership (`@security.isOwner`) |
| Errors | `ProblemDetailHandler`, `ProblemDetailSecurityHandlers` | RFC 9457 bodies with a stable `code` and the request id |
| Security | `JwtAuthFilter`, `JwtService`, `SecurityConfig`, `JwtSecretValidator` | Stateless JWT; prod refuses weak or published secrets |
| Operations | `CorrelationIdFilter`, `CatalogStartupCheck`, Actuator | `X-Request-Id` everywhere; bad catalog data stops startup; metrics for admins |
