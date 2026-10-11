-- V6: progress reflects reality (BR-PRG-01..06).

-- ── Meals: the user checks off what they ate (BR-PRG-05) ─────────────────────
alter table meals
    add column completed    boolean not null default false,
    add column completed_at timestamp(6),
    add constraint ck_meals_completed_at check (completed or completed_at is null);

-- ── Plans: the calorie target the plan was built for (BR-PRG-06) ────────────
-- Earlier plans did not record it; their planned total is the closest value available.
alter table diet_plans add column target_calories integer;
update diet_plans set target_calories = total_calories where target_calories is null;
alter table diet_plans add constraint ck_diet_plans_target check (target_calories > 0);

-- ── Progress entries ─────────────────────────────────────────────────────────
alter table progress_entries
    add column target_calories     integer,
    add column calories_overridden boolean not null default false;

-- Earlier values outside the allowed ranges are treated as unknown (as BR-PRF-01 does for profiles)
update progress_entries set weight_kg = null where weight_kg < 30 or weight_kg > 300;
update progress_entries set calories_consumed = null where calories_consumed > 10000;
update progress_entries set water_ml = null where water_ml > 10000;
update progress_entries set exercise_minutes = null where exercise_minutes > 1440;
update progress_entries set notes = left(notes, 1000) where length(notes) > 1000;

-- Calories typed by hand before meal check-off existed are the user's own number: keep them as overrides
update progress_entries set calories_overridden = true where calories_consumed > 0;

-- mealsCompleted is now derived from checked meals; no meal was checked before this version
update progress_entries set meals_completed = 0;
alter table progress_entries
    alter column meals_completed set default 0,
    alter column meals_completed set not null;

-- Target snapshot: the ACTIVE plan for that day, where there is one
update progress_entries e
set target_calories = p.target_calories
from diet_plans p
where p.user_id = e.user_id and p.valid_for_date = e.date and p.status = 'ACTIVE';

-- Bounds, mirroring the application rules as defense in depth (BR-PRG-03)
alter table progress_entries
    drop constraint ck_progress_calories,
    drop constraint ck_progress_water,
    drop constraint ck_progress_meals,
    drop constraint ck_progress_exercise,
    add constraint ck_progress_calories check (calories_consumed between 0 and 10000),
    add constraint ck_progress_water check (water_ml between 0 and 10000),
    add constraint ck_progress_meals check (meals_completed between 0 and 5),
    add constraint ck_progress_exercise check (exercise_minutes between 0 and 1440),
    add constraint ck_progress_weight check (weight_kg between 30 and 300),
    add constraint ck_progress_notes check (length(notes) <= 1000),
    add constraint ck_progress_target check (target_calories > 0);
