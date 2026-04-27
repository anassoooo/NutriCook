# NutriCook — Sequence Diagrams

**Version**: 1.0.0 | **Date**: 2026-04-26

---

## SD-01 — User Registration & Login (JWT Flow)

```mermaid
sequenceDiagram
    actor Guest
    participant FE as Frontend (React)
    participant Auth as AuthController
    participant AS as AuthService
    participant JwtSvc as JwtService
    participant DB as Database (H2/Supabase)

    Note over Guest, DB: Registration
    Guest->>FE: Fill email + password form
    FE->>Auth: POST /api/auth/register {email, password}
    Auth->>AS: register(request)
    AS->>DB: existsByEmail(email)
    DB-->>AS: false

    AS->>AS: BCrypt.encode(password, strength=10)
    AS->>DB: save(User{email, passwordHash, role=USER})
    DB-->>AS: User{id=1}
    AS->>DB: save(UserProfile{userId=1})
    AS->>JwtSvc: generateToken(user)
    JwtSvc-->>AS: "eyJ..."

    AS-->>Auth: AuthResponse{token, email, role}
    Auth-->>FE: 201 {token, email, role}
    FE->>FE: localStorage.setItem("jwt", token)
    FE-->>Guest: Redirect to Profile Setup

    Note over Guest, DB: Login (subsequent visits)
    Guest->>FE: Submit email + password
    FE->>Auth: POST /api/auth/login {email, password}
    Auth->>AS: login(request)
    AS->>DB: findByEmail(email)
    DB-->>AS: User
    AS->>AS: BCrypt.matches(password, hash)
    AS->>JwtSvc: generateToken(user)
    JwtSvc-->>AS: "eyJ..."
    AS-->>Auth: AuthResponse{token}
    Auth-->>FE: 200 {token, email, role}
    FE->>FE: localStorage.setItem("jwt", token)
    FE-->>Guest: Redirect to Dashboard
```

---

## SD-02 — Generate Diet Plan (Core Flow)

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend (React)
    participant Filter as JwtAuthFilter
    participant DC as DietController
    participant DPS as DietPlanService
    participant NS as NutritionService
    participant AI as GroqAiService
    participant Groq as Groq API
    participant DB as Database

    User->>FE: Click "Generate Plan" for today
    FE->>Filter: POST /api/diet/generate?date=2026-04-26
    Note over FE, Filter: Authorization: Bearer eyJ...

    Filter->>Filter: extractEmail(token)
    Filter->>DB: loadUserByEmail(email)
    DB-->>Filter: UserDetails
    Filter->>Filter: isTokenValid → true
    Filter->>DC: forward authenticated request

    DC->>DPS: generatePlan(userId=1, date=2026-04-26)

    DPS->>DB: findByUserIdAndValidForDateAndStatus(1, date, ACTIVE)
    DB-->>DPS: existingPlan (if any)
    DPS->>DB: archive existing plan (status = ARCHIVED)

    DPS->>DB: findByUserId(1) [UserProfile]
    DB-->>DPS: UserProfile{weight, height, activityLevel, healthGoal, restrictions, conditions}

    DPS->>DPS: computeTDEE(profile) → 2100 kcal

    DPS->>DB: findCompatibleFoods(restrictionIds)
    DB-->>DPS: List~FoodItem~

    DPS->>NS: validateAgainstConditions(foods, conditions)
    NS-->>DPS: filteredFoods

    DPS->>DPS: buildMeals(filteredFoods, targetCalories=2100)
    Note over DPS: 5 meals: Breakfast 500, Snack 200,<br/>Lunch 600, Snack 200, Dinner 600

    loop For each Meal (5 total)
        DPS->>AI: getMealSuggestion(meal, profile)
        AI->>AI: loadPrompt("meal-suggestion.st")
        AI->>Groq: POST /openai/v1/chat/completions
        Groq-->>AI: suggestion text
        AI-->>DPS: "Try grilled chicken with..."
        DPS->>DPS: meal.aiSuggestion = suggestion
    end

    DPS->>AI: getPlanExplanation(plan, profile)
    AI->>Groq: POST /openai/v1/chat/completions
    Groq-->>AI: explanation text
    AI-->>DPS: "This plan is designed for..."

    DPS->>NS: computeTotals(meals)
    NS-->>DPS: NutritionTotals{2100 kcal, 140g protein...}

    DPS->>DB: save(DietPlan{status=ACTIVE, meals, totals, aiExplanation})
    DB-->>DPS: DietPlan{id=42}

    DPS-->>DC: DietPlan
    DC-->>FE: 201 DietPlanResponse{id, meals, nutrition, aiExplanation}
    FE-->>User: Render 3D diet plan page
```

---

## SD-03 — Find Nearby Restaurants (OSM Flow)

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend (React + Leaflet)
    participant LC as LocationController
    participant LS as LocationService
    participant Cache as Caffeine Cache
    participant OV as OverpassClient
    participant OSM as Overpass API (OSM)

    User->>FE: Click "Nearby Restaurants"
    FE->>FE: navigator.geolocation.getCurrentPosition()
    FE->>FE: lat=36.8606, lng=10.2087

    FE->>LC: GET /api/location/restaurants?lat=36.86&lng=10.21&restrictions=vegan
    LC->>LS: getNearbyRestaurants(36.86, 10.21, ["vegan"])

    LS->>Cache: get("restaurants:36.86:10.21:vegan")
    Cache-->>LS: null (cache miss)

    LS->>LS: buildOverpassQuery(lat, lng, "restaurant")
    Note over LS: [out:json]; node["amenity"="restaurant"]<br/>(around:2000, 36.86, 10.21); out body;

    LS->>OV: query(overpassQL)
    Note over OV: User-Agent: NutriCook / anasbougrine63@gmail.com
    OV->>OSM: POST https://overpass-api.de/api/interpreter
    OSM-->>OV: {elements: [{id, lat, lon, tags}...]}
    OV-->>LS: OverpassResponse

    LS->>LS: map to List~LocationDTO~
    LS->>LS: filter by restrictions (tag matching)
    LS->>Cache: put("restaurants:36.86:10.21:vegan", results, TTL=1h)

    LS-->>LC: List~LocationDTO~
    LC-->>FE: 200 [{name, lat, lng, address, osmNodeId}...]

    FE->>FE: Render markers on Leaflet map
    FE-->>User: Interactive map with restaurant pins
```

---

## SD-04 — Daily Progress Log + Achievement Unlock

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend (React)
    participant PC as ProgressController
    participant PS as ProgressService
    participant AS as AchievementService
    participant DB as Database

    User->>FE: Submit progress form {date, calories, water, meals, weight}
    FE->>PC: POST /api/progress {date, caloriesConsumed=1950, waterMl=2500, mealsCompleted=5}

    PC->>PS: logProgress(userId=1, request)

    PS->>DB: findByUserIdAndDate(1, 2026-04-26)
    DB-->>PS: null (no entry today)

    PS->>DB: save(ProgressEntry{userId=1, date, calories=1950, water=2500, meals=5})
    DB-->>PS: ProgressEntry{id=77}

    PS->>AS: checkAndAward(userId=1)

    AS->>DB: findByUserIdAndDateBetween(1, last7days)
    DB-->>AS: [7 entries, each with mealsCompleted >= 1]
    AS->>AS: evaluateStreak(userId) → 7 consecutive days

    AS->>DB: findByType(LOGIN_STREAK)
    DB-->>AS: [Achievement{name="Week Warrior", threshold=7}]

    AS->>DB: existsByUserIdAndAchievementId(1, achievementId)
    DB-->>AS: false (not yet earned)

    AS->>DB: save(UserAchievement{userId=1, achievementId, earnedAt=now})
    DB-->>AS: UserAchievement{id=12}

    AS-->>PS: [UserAchievement{name="Week Warrior"}]
    PS-->>PC: ProgressEntry + newAchievements

    PC-->>FE: 200 {progress, newAchievements: ["Week Warrior"]}
    FE-->>User: Show progress saved + 🏆 "Week Warrior" unlocked toast
```
