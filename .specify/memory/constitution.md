<!--
==============================================================================
SYNC IMPACT REPORT
==============================================================================
Version change: 1.1.0 → 2.0.0
Bump rationale: MAJOR. The governing priority changed from "stack and visuals"
  to "business correctness". Principles were reordered and redefined. Rules
  the code never followed (UML-before-entity, H2 for integration tests,
  80% overall coverage) were replaced with rules that are enforced
  mechanically.

Modified principles:
  I.    Domain Exclusivity       → kept as Scope Discipline (now X)
  II.   Zero-Cost Operation      → kept (now IX)
  III.  Modern Java Backend      → split into IV (Pure Domain Core) and the stack table
  IV.   Immersive 3D Frontend    → reduced to VIII (Frontend Displays, Backend Decides)
  V.    UML-Driven Data Model    → replaced by VI (Migrations Own the Schema)
  VI.   Groq AI Integration      → redefined as III (AI Advises, Domain Decides)
  VII.  OpenStreetMap-First      → moved into the stack table + IX
  VIII. Stateless JWT Auth       → merged into VII (Ownership & Least Privilege)

Added principles:
  I.  Business Rules Are the Product
  II. Safety Fails Closed
  V.  Every Rule Is Tested by Its ID

Added artifacts:
  docs/domain/business-rules.md: canonical rule catalogue (BR-* IDs)
  specs/001-reliable-core/{spec,plan,tasks}.md

Removed:
  "Environment Files" section (described files that do not match the repo;
  replaced by .env.example, task T004)

Templates requiring updates:
  ✅ specs/001-reliable-core/plan.md: Constitution Check uses principles I–X
  ✅ .specify/templates/plan-template.md: Constitution Check table for I–X (T082)
  ✅ .specify/templates/tasks-template.md: mandatory tests, Rules and Done-when lines (T082)

Deferred TODOs: none
==============================================================================
-->

# NutriCook Constitution

NutriCook builds personalized daily meal plans from a user's body data, goals,
allergies and health conditions, then helps them follow those plans through
progress tracking, gamification and an AI assistant.

**What we optimize for, in order**:
1. **Correctness.** The numbers are right.
2. **Safety.** No user is shown food they must not eat.
3. **Reliability.** The system degrades gracefully.
4. **Experience.** It feels good to use.

When two principles conflict, the one listed first wins.

## Core Principles

### I. Business Rules Are the Product

The canonical description of how NutriCook behaves is
`docs/domain/business-rules.md`, not the code and not the UI.

- Every behavior that affects a number, an eligibility decision, a state
  transition or a reward MUST be written there as a rule with a stable
  `BR-<AREA>-<NN>` ID before (or in the same PR as) its implementation.
- If code and catalogue disagree, the code is defective until the catalogue
  is amended through governance.
- Rule IDs are permanent. A retired rule is marked `RETIRED`, never deleted
  or renumbered.
- Every tunable number (thresholds, ratios, limits) MUST be a named constant
  bound from `application.yml` via a validated `@ConfigurationProperties`
  record, and listed in the catalogue's constants table. Magic numbers in
  services and components are prohibited.

**Rationale**: A diet app is only as good as its arithmetic and its rules. An
explicit catalogue makes the logic reviewable by non-programmers (a
dietitian, a jury), testable by ID, and stable across refactors.

### II. Safety Fails Closed

Allergies, intolerances, diet/religious restrictions and health-condition
nutrient limits are **hard constraints**.

- A hard constraint MUST NOT be relaxed to "make a plan work". If it cannot
  be satisfied, the operation fails with an explicit error code
  (`PLAN_INFEASIBLE`, `SWAP_REJECTED`) naming the constraint.
- Unknown data counts as unsafe: a food with no allergen data is ineligible
  for any user with an allergy or intolerance.
- Every code path that creates or changes a plan (generation, manual swap,
  AI swap, admin edit) MUST pass through a single `PlanSafetyValidator`
  immediately before persistence. There are no alternative write paths.
- Eligibility MUST be decided from structured attributes (allergens, diet
  flags, nutrient values), never from free-text names or category strings.
- The app MUST NOT present itself as medical advice. Health-condition users
  see a disclaimer.

**Rationale**: Showing peanut butter to a user with a nut allergy is the one
bug that can actually hurt someone. Failing closed is always recoverable;
failing open may not be.

### III. AI Advises, Domain Decides

The LLM is a language layer, not a source of truth.

- The AI MUST NOT produce any value that is persisted as nutrition data,
  portion, target or eligibility decision. Those come only from the domain.
- The AI MAY produce: explanations, tips, chat replies, and **intents**
  (e.g. `{mealType, foodId}`) chosen from a candidate list the domain built
  from the user's eligible foods. Every intent is re-validated by the domain
  before any effect.
- AI output MUST be parsed against a schema (Spring AI structured output).
  Free-form string scraping is prohibited.
- AI calls MUST run outside database transactions, with a timeout, and
  failure MUST degrade the feature, never fail a core operation (plan
  generation succeeds without AI text).
- User text sent to the model is length-capped, delimited and treated as
  data. Prompts live in `src/main/resources/prompts/` with a version header.
- Per-user quotas protect the free tier (see catalogue constants).

**Rationale**: LLMs are excellent at language and unreliable at arithmetic
and policy. Constraining them to proposals keeps the product deterministic,
testable and safe while still feeling intelligent.

### IV. Pure, Deterministic Domain Core

Business logic lives in `com.nutricook.domain..` as plain Java.

- Domain classes MUST NOT depend on Spring, JPA, HTTP, the AI client or the
  system clock. They take value objects and return value objects. Time comes
  from an injected `java.time.Clock`, and randomness from a seeded
  `RandomGenerator`.
- Same inputs MUST give the same outputs (plan generation is seeded by
  user, date and generation sequence).
- Application services (`com.nutricook.service`) orchestrate repositories,
  domain and AI. Controllers stay thin: validate, delegate, map.
- Invariants (totals = sum of components, one ACTIVE plan per user-day,
  legal state transitions) are enforced in the domain, not in controllers or
  the UI.
- These boundaries are enforced by ArchUnit tests in the build.

**Rationale**: Pure functions are trivially testable, property-testable and
explainable. Determinism turns "it generated something weird once" into a
reproducible bug report.

### V. Every Rule Is Tested by Its ID

- Each BR rule MUST have at least one automated test whose method name
  starts with its ID (`BR_SAF_04_...`). A CI script fails the build if any
  non-retired rule in the catalogue has no matching test.
- The catalogue's reference cases (e.g. energy cases A–D) are exact-value
  acceptance tests.
- Safety and invariant rules MUST additionally have **property-based tests**
  (jqwik) over randomized profiles and restriction sets.
- Domain package line coverage ≥ 90% and service package ≥ 75%, enforced by
  JaCoCo in `mvn verify`.
- Integration tests run against **PostgreSQL via Testcontainers**, the same
  engine as production. Local development also uses PostgreSQL (Docker Compose).
- A bug fix starts with a failing test that reproduces it.

**Rationale**: "Tested" must be checkable by a machine. Tying tests to rule
IDs gives traceability from requirement to proof, which is exactly what a
reviewer or jury asks for.

### VI. Migrations Own the Schema

- The schema is defined by Flyway migrations in
  `backend/src/main/resources/db/migration`. Hibernate runs with
  `ddl-auto: validate` in **every** profile.
- Reference data (foods, restrictions, conditions, achievements) is seeded
  by migrations, not by application runners. Demo accounts are created only
  in the `local` profile with credentials from the environment.
- Persisted plans and progress entries are **snapshots**. Catalog or profile
  edits never rewrite history, and catalog items are soft-deleted.
- The data model diagram (`docs/diagrams/class-data.mmd`) is updated in the
  same PR as any migration that changes tables.

**Rationale**: Schema drift between H2 `update` mode and production is the
most common way student projects fail on deployment day.

### VII. Ownership & Least Privilege

- Authentication is stateless JWT (HMAC-SHA256, secret ≥ 256 bits from
  `JWT_SECRET`). The app MUST refuse to start in `prod` with a missing or
  default secret.
- Every user-scoped resource is authorized by ownership (`@security.isOwner`)
  **and** the service verifies the aggregate belongs to that user.
  Admin-only operations require `ROLE_ADMIN`.
- Errors are RFC 9457 `ProblemDetail` with a stable `code`. Exception
  messages, stack traces and SQL never reach clients.
- CORS allows only configured origins. The H2 console and Swagger are
  disabled in `prod`.
- Secrets never appear in the repository, logs, or AI prompts.

**Rationale**: Health data is personal data. Least privilege and opaque
errors are baseline professional practice.

### VIII. Frontend Displays, Backend Decides

- The React client MUST NOT compute or hard-code targets, eligibility,
  achievement progress or plan totals. It renders what the API returns.
- Every API error code has a defined user-facing message. No raw error
  strings are shown.
- Visual richness (R3F, Framer Motion) is welcome, but every 3D scene has a
  `prefers-reduced-motion` fallback, and the UI meets WCAG 2.1 AA contrast
  and keyboard navigation.

**Rationale**: When the same rule lives in two places it eventually
disagrees with itself. The client is a view.

### IX. Zero-Cost, Reproducible Operation

- Every runtime dependency MUST have a permanently free tier or be
  open-source (Groq, OSM Nominatim/Overpass, Neon/Supabase Postgres, Render/
  Fly, Vercel). Paid services require explicit owner approval.
- Rate-limited free services MUST degrade gracefully and be cached where
  their policy requires (OSM ≥ 1 h).
- A fresh clone MUST run with `docker compose up` plus a filled `.env` copied
  from `.env.example`. CI MUST build and test every push.

**Rationale**: The project is judged by people who will clone it once. It
has to work the first time, for free.

### X. Scope Discipline

- Every feature MUST serve diet planning, nutrition tracking, or healthy-
  eating guidance. Anything else is rejected at spec review.
- Work is delivered as Spec Kit features (`specs/NNN-name/`) with spec → plan
  → tasks. Each task names exact files and has a verifiable "Done when".
- Prefer finishing and hardening existing features over adding new ones.

**Rationale**: A small, correct, well-tested product beats a broad,
half-working one on a CV and in a defense.

## Technology Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend | Java 21, Spring Boot 3.3, Maven | `backend/` |
| AI | Spring AI 1.0 (OpenAI adapter) → Groq `openai/gpt-oss-120b` | model configurable |
| Security | Spring Security 6, JJWT 0.12 | stateless |
| Persistence | Spring Data JPA, **PostgreSQL 16**, Flyway | all environments; H2 retired |
| Caching / limits | Caffeine, Bucket4j | OSM cache, AI quotas |
| API docs | springdoc-openapi 2 | disabled in prod |
| Testing | JUnit 5, Mockito, AssertJ, jqwik, Testcontainers, ArchUnit, JaCoCo | |
| Frontend | React 18, TypeScript 5, Vite 5, Tailwind 3, Framer Motion, R3F | `frontend/` |
| Frontend testing | Vitest, Testing Library, MSW | |
| Geo | OSM Nominatim + Overpass | User-Agent required |
| Delivery | Docker Compose, GitHub Actions | |

## Development Workflow

- **Branches**: `main` is always releasable. Feature work happens on `NNN-feature-name`
  (Spec Kit numbering), fixes on `fix/<desc>`.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
  Reference BR IDs and task IDs where relevant (`fix(BR-SAF-04): …  [T031]`).
- **Quality gates** (all MUST pass in CI before merge):
  1. `./mvnw verify`: tests, JaCoCo thresholds, ArchUnit, fmt check
  2. BR coverage script: every active BR ID has a test
  3. `npm run typecheck && npm run lint && npm test && npm run build`
  4. Flyway migrations apply cleanly to an empty PostgreSQL
  5. Catalogue and diagram updated if rules or schema changed
- **Code style**: google-java-format (fmt-maven-plugin); ESLint + Prettier.

## Governance

This constitution supersedes all other project conventions. The business
rules catalogue is subordinate to it, and feature specs are subordinate to
both.

**Amendment procedure**: edit this file, bump the version, update the Sync
Impact Report, update dependent templates and specs, and describe the
change in the commit body.

**Versioning**: MAJOR = principle removed, redefined, or reprioritized;
MINOR = principle or mandatory section added; PATCH = wording only.

**Compliance**: every `plan.md` contains a Constitution Check covering
principles I–X. A violation is allowed only if it is justified in that
plan's Complexity Tracking table.

**Version**: 2.0.0 | **Ratified**: 2026-04-26 | **Last Amended**: 2026-10-10
