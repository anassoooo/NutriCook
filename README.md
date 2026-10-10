# NutriCook — AI-Powered Diet Assistant

[![CI](https://github.com/anassoooo/NutriCook/actions/workflows/ci.yml/badge.svg)](https://github.com/anassoooo/NutriCook/actions/workflows/ci.yml)

A full-stack web application that generates personalized AI meal plans, tracks macros and progress, awards achievements, and finds nearby healthy restaurants — all at zero cost.

---

## Features

- **AI Meal Plan Generation** — Groq AI (Llama 3.3 70B) creates a full daily meal plan tailored to your profile, health goal, and dietary restrictions
- **Macro & Calorie Tracking** — animated SVG donut rings for Protein, Carbs, Fat, Sugar and Calories
- **Progress Logging** — daily weight, calories, and exercise minutes with streak tracking
- **Achievements System** — gamified milestones that unlock as you hit health targets
- **Nearby Healthy Restaurants** — powered by OpenStreetMap / Nominatim (free, no API key needed)
- **JWT Authentication** — stateless, secure sign-up and sign-in
- **3D Nutrition Orb** — React Three Fiber sphere in the dashboard hero that reflects calorie fill

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vite + React 18 + TypeScript |
| Styling | Tailwind CSS 3.4, Framer Motion 11 |
| 3D | React Three Fiber 8 + Drei |
| Backend | Spring Boot 3.3.4 + Spring AI 1.0.0 |
| AI Model | Groq API — Llama 3.3 70B Versatile |
| Auth | JJWT 0.12.3, Spring Security 6 |
| Database | PostgreSQL 16 (Docker / prod), H2 (local dev) |
| Maps | OpenStreetMap + Nominatim (free) |

---

## Architecture

```
┌─────────────────────────────────────┐
│           Browser (Vite)            │
│  React + Tailwind + R3F + Framer    │
└──────────────┬──────────────────────┘
               │ REST /api/*  (JWT Bearer)
┌──────────────▼──────────────────────┐
│       Spring Boot 3.3.4             │
│  Spring Security · Spring AI        │
│  JPA / Hibernate · H2 / PostgreSQL  │
└──────┬───────────────┬──────────────┘
       │               │
┌──────▼──────┐  ┌─────▼───────────────┐
│  Groq API   │  │  Nominatim OSM API  │
│ Llama 3.3   │  │  (restaurants)      │
└─────────────┘  └─────────────────────┘
```

---

## Quickstart

### Option A: Docker (recommended, one command)

Prerequisites: Docker Desktop and a free [Groq API key](https://console.groq.com) (optional; without it, AI text is unavailable).

```bash
git clone https://github.com/anassoooo/NutriCook.git
cd NutriCook
cp .env.example .env        # then set GROQ_API_KEY (and a real JWT_SECRET outside local use)
docker compose up --build
```

| Service | URL |
|---|---|
| App (nginx + React) | http://localhost:8081 |
| API + Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | `localhost:5433` (user/password `nutricook`) |

Stop with `docker compose down` (add `-v` to wipe the database).

### Option B: Local development

Prerequisites: Java 21, Node.js 24+. Maven is not needed because the wrapper is included.

```bash
# Backend (local profile, file-based H2, port 8080)
cd backend
export GROQ_API_KEY=...            # PowerShell: $env:GROQ_API_KEY="..."
./mvnw spring-boot:run             # Windows: mvnw.cmd spring-boot:run

# Frontend (Vite dev server on 5173, proxies /api → 8080)
cd frontend
npm ci
npm run dev
```

### Quality checks

```bash
cd backend  && ./mvnw verify                 # tests + google-java-format
cd frontend && npm run typecheck && npm run lint && npm test && npm run build
```

The same checks run in GitHub Actions on every push ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

---

## Environment Variables

All variables are documented in [`.env.example`](.env.example).

| Variable | Required | Description |
|---|---|---|
| `GROQ_API_KEY` | For AI features | From [console.groq.com](https://console.groq.com) |
| `JWT_SECRET` | Yes (prod) | Base64 secret, ≥ 32 bytes: `openssl rand -base64 48` |
| `SPRING_PROFILES_ACTIVE` | No | `local` (H2), `docker` (Compose Postgres), `prod` |
| `DATABASE_URL` / `_USERNAME` / `_PASSWORD` | `docker`, `prod` | JDBC PostgreSQL connection |

---

## Project Structure

```
NutriCook/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/nutricook/
│       ├── controller/          # REST endpoints
│       ├── service/             # Business logic + Groq AI
│       ├── entity/              # JPA entities
│       ├── security/            # JWT filter + Spring Security
│       └── config/              # CORS, cache, app config
├── frontend/
│   ├── src/
│   │   ├── pages/               # Auth, Dashboard, DietPlan, Progress, Achievements, Restaurants
│   │   ├── components/          # Navbar, MacroRing, 3D NutritionOrb
│   │   ├── contexts/            # AuthContext (JWT storage)
│   │   └── types/               # Shared TypeScript interfaces
│   └── vite.config.ts           # /api proxy → localhost:8080
├── docs/uml/                    # Class, sequence, use-case diagrams
└── legacy/                      # Original Flask prototype (reference only)
```

---

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Sign in → JWT |
| GET/PUT | `/api/users/{id}/profile` | User profile |
| GET/POST | `/api/users/{id}/diet-plans` | List / generate plan |
| GET/POST | `/api/users/{id}/progress` | Progress entries |
| GET | `/api/users/{id}/achievements` | Achievements |
| GET | `/api/locations/search` | Nearby restaurants (OSM) |

All protected endpoints require `Authorization: Bearer <token>`.

---

## Screenshots

> Coming soon — run the app and take screenshots of your own data.

---

## License

MIT — see [LICENSE](LICENSE).
