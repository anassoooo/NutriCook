-- V8: curated catalog administration (BR-CAT-01/04/05).

-- SEED foods come from db/catalog/foods.csv (R__food_catalog.sql); ADMIN foods from the admin API.
-- The seed migration only ever touches SEED rows, so it can never overwrite an admin's food.
alter table food_items
    add column origin varchar(8) not null default 'SEED',
    add constraint ck_food_items_origin check (origin in ('SEED', 'ADMIN'));

-- Names are unique regardless of case: "Tofu" and "tofu" are the same food (BR-CAT-01)
create unique index ux_food_items_name_ci on food_items (lower(name));

-- The admin list filters by role and active and searches by name
create index ix_food_items_role_active on food_items (role, active);
