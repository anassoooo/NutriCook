# NutriCook — AI-Powered Diet Assistant

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
| Database | H2 (dev) / PostgreSQL (prod) |
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

### Prerequisites

- Java 21 (Microsoft / Temurin / Zulu)
- Maven 3.9+ (or use the included `mvnw.cmd`)
- Node.js 20+
- A free [Groq API key](https://console.groq.com)

### 1. Clone

```bash
git clone https://github.com/anassoooo/NutriCook.git
cd NutriCook
```

### 2. Backend

Copy the run script template and fill in your values:

```bash
cp backend/run.bat.example backend/run.bat
```

Edit `backend/run.bat` and set:

```bat
set "GROQ_API_KEY=your_groq_api_key_here"
set "JWT_SECRET=your_base64_secret_here"
set "JAVA_HOME=C:\path\to\your\jdk21"
set "M2_HOME=C:\path\to\your\maven"
```

Then start the backend:

```bash
cd backend
run.bat
```

The API will be available at `http://localhost:8080`.

#### Generate a JWT secret

```bash
openssl rand -base64 32
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `GROQ_API_KEY` | Yes | From [console.groq.com](https://console.groq.com) |
| `JWT_SECRET` | Yes | Base64-encoded secret (min 32 bytes) |
| `SPRING_PROFILES_ACTIVE` | No | `local` (H2) or `prod` (PostgreSQL) |

No `.env` file needed for the frontend — the Vite dev server proxies `/api` to `localhost:8080` automatically.

---

## Project Structure

```
NutriCook/
├── backend/
│   ├── run.bat.example          # Copy → run.bat and fill secrets
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
