-- V5: plan lifecycle (BR-LIF-01/02/06/07), determinism (BR-CMP-07) and snapshots (BR-LIF-05).

-- ── Plans ────────────────────────────────────────────────────────────────────
alter table diet_plans
    add column generation_seq  integer not null default 1,
    add column seed            bigint,
    add column warnings        jsonb not null default '[]'::jsonb,
    add column ai_status       varchar(16) not null default 'READY',
    add column total_fiberg    float(53),
    add column total_sodium_mg float(53),
    drop constraint ck_diet_plans_status,
    add constraint ck_diet_plans_status check (status in ('ACTIVE', 'SUPERSEDED', 'ARCHIVED')),
    add constraint ck_diet_plans_ai_status check (ai_status in ('PENDING', 'READY', 'UNAVAILABLE'));

-- Should earlier data hold several ACTIVE plans for one day, keep the newest (BR-LIF-02)
update diet_plans p
set status = 'SUPERSEDED'
where p.status = 'ACTIVE'
  and exists (select 1 from diet_plans q
              where q.user_id = p.user_id and q.valid_for_date = p.valid_for_date
                and q.status = 'ACTIVE' and q.id > p.id);

-- At most one ACTIVE plan per user and day, enforced by the database under concurrency (plan D5)
create unique index ux_active_plan on diet_plans (user_id, valid_for_date) where status = 'ACTIVE';

-- Quota counting (BR-LIF-06)
create index ix_diet_plans_user_generated on diet_plans (user_id, generated_at);

-- ── Meals ────────────────────────────────────────────────────────────────────
alter table meals add column sodium_mg float(53);

-- ── Components: role + nutrition snapshot, so catalog edits never rewrite history (BR-LIF-05) ──
alter table meal_food_items
    add column role       varchar(16),
    add column kcal       float(53),
    add column protein_g  float(53),
    add column carbs_g    float(53),
    add column fat_g      float(53),
    add column sugar_g    float(53),
    add column fiber_g    float(53),
    add column sodium_mg  float(53);

update meal_food_items mfi
set role      = f.role,
    kcal      = round(cast(mfi.quantityg * f.calories_per100g / 100 as numeric), 1),
    protein_g = round(cast(mfi.quantityg * f.protein_per100g / 100 as numeric), 1),
    carbs_g   = round(cast(mfi.quantityg * f.carbs_per100g / 100 as numeric), 1),
    fat_g     = round(cast(mfi.quantityg * f.fat_per100g / 100 as numeric), 1),
    sugar_g   = round(cast(mfi.quantityg * coalesce(f.sugar_per100g, 0) / 100 as numeric), 1),
    fiber_g   = round(cast(mfi.quantityg * coalesce(f.fiber_per100g, 0) / 100 as numeric), 1),
    sodium_mg = round(cast(mfi.quantityg * coalesce(f.sodium_mg_per100g, 0) / 100 as numeric), 1)
from food_items f
where f.id = mfi.food_item_id;

alter table meal_food_items
    alter column role set not null,
    alter column kcal set not null,
    alter column protein_g set not null,
    alter column carbs_g set not null,
    alter column fat_g set not null,
    alter column sugar_g set not null,
    alter column fiber_g set not null,
    alter column sodium_mg set not null,
    add constraint ck_mfi_role check (role in ('PROTEIN', 'CARB', 'PRODUCE', 'FAT'));
