-- V3: structured safety and composition attributes (BR-SAF-01..03, BR-CAT-01, BR-CMP-02..04).
-- Eligibility is decided from these attributes, never from names or the free-text category.

-- ── Foods ────────────────────────────────────────────────────────────────────
alter table food_items
    add column role              varchar(16),
    add column state             varchar(16),
    add column portion_min_g     float(53),
    add column portion_max_g     float(53),
    add column portion_step_g    float(53),
    add column default_portion_g float(53),
    add column source            varchar(255),
    add column active            boolean not null default true,
    add column allergens_known   boolean not null default false,
    add column meat              boolean not null default false;

-- Rows seeded before the curated catalog existed are unverified (no allergen data): they are
-- deactivated here and only come back if R__food_catalog.sql re-sources them by name (BR-SAF-03).
update food_items
set role = case category
               when 'meat' then 'PROTEIN'
               when 'seafood' then 'PROTEIN'
               when 'egg' then 'PROTEIN'
               when 'dairy' then 'PROTEIN'
               when 'legume' then 'PROTEIN'
               when 'grain' then 'CARB'
               when 'nut' then 'FAT'
               when 'fat' then 'FAT'
               else 'PRODUCE' end,
    state = 'RAW',
    portion_min_g = 50,
    portion_max_g = 250,
    portion_step_g = 5,
    default_portion_g = 100,
    active = false,
    allergens_known = false,
    meat = (category = 'meat');

alter table food_items
    alter column role set not null,
    alter column state set not null,
    alter column portion_min_g set not null,
    alter column portion_max_g set not null,
    alter column portion_step_g set not null,
    alter column default_portion_g set not null,
    add constraint ck_food_items_role check (role in ('PROTEIN', 'CARB', 'PRODUCE', 'FAT')),
    add constraint ck_food_items_state check (state in ('RAW', 'COOKED', 'DRY')),
    add constraint ck_food_items_portion
        check (portion_min_g > 0 and portion_min_g <= default_portion_g
            and default_portion_g <= portion_max_g and portion_step_g > 0);

create table food_allergens (
    food_id  bigint      not null,
    allergen varchar(16) not null,
    primary key (food_id, allergen),
    constraint fk_food_allergens_food foreign key (food_id) references food_items (id) on delete cascade,
    constraint ck_food_allergens check (allergen in ('GLUTEN', 'CRUSTACEANS', 'EGGS', 'FISH', 'PEANUTS',
        'SOY', 'MILK', 'TREE_NUTS', 'CELERY', 'MUSTARD', 'SESAME', 'SULPHITES', 'LUPIN', 'MOLLUSCS'))
);

create table food_diet_flags (
    food_id   bigint      not null,
    diet_flag varchar(16) not null,
    primary key (food_id, diet_flag),
    constraint fk_food_diet_flags_food foreign key (food_id) references food_items (id) on delete cascade,
    constraint ck_food_diet_flags check (diet_flag in ('VEGAN', 'VEGETARIAN', 'HALAL', 'KOSHER'))
);

create table food_suitable_slots (
    food_id bigint      not null,
    slot    varchar(16) not null,
    primary key (food_id, slot),
    constraint fk_food_slots_food foreign key (food_id) references food_items (id) on delete cascade,
    constraint ck_food_slots check (slot in ('BREAKFAST', 'MORNING_SNACK', 'LUNCH', 'AFTERNOON_SNACK', 'DINNER'))
);

-- ── Dietary restrictions → structured exclusions (BR-SAF-02) ─────────────────
alter table dietary_restrictions
    add column required_diet_flag varchar(16),
    add constraint ck_restrictions_flag
        check (required_diet_flag is null or required_diet_flag in ('VEGAN', 'VEGETARIAN', 'HALAL', 'KOSHER'));

create table restriction_excluded_allergens (
    restriction_id bigint      not null,
    allergen       varchar(16) not null,
    primary key (restriction_id, allergen),
    constraint fk_rea_restriction foreign key (restriction_id) references dietary_restrictions (id) on delete cascade
);

-- ── Health conditions → nutrient limits and implied exclusions (BR-SAF-06, BR-PRF-07) ──
alter table health_conditions rename column sugar_limitg to sugar_limit_per_meal_g;
alter table health_conditions rename column fat_limitg to fat_limit_per_meal_g;
alter table health_conditions add column sodium_limit_per_day_mg float(53);

create table condition_implied_allergens (
    condition_id bigint      not null,
    allergen     varchar(16) not null,
    primary key (condition_id, allergen),
    constraint fk_cia_condition foreign key (condition_id) references health_conditions (id) on delete cascade
);
