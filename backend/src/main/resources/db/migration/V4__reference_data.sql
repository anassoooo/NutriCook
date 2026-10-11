-- V4: reference data owned by migrations (Constitution VI), previously seeded by DataInitializer.
-- Upserts by name, so it works on fresh databases and on databases seeded by the old initializer.
-- Foods are seeded by the repeatable migration R__food_catalog.sql.

-- ── Dietary restrictions (BR-SAF-02) ─────────────────────────────────────────
insert into dietary_restrictions (name, type, description) values
    ('Vegan',             'PREFERENCE',  'No animal products'),
    ('Vegetarian',        'PREFERENCE',  'No meat or fish'),
    ('Gluten-Free',       'INTOLERANCE', 'No gluten-containing foods'),
    ('Lactose-Free',      'INTOLERANCE', 'No dairy products'),
    ('Nut Allergy',       'ALLERGY',     'No tree nuts or peanuts'),
    ('Shellfish Allergy', 'ALLERGY',     'No crustaceans or molluscs'),
    ('Egg Allergy',       'ALLERGY',     'No eggs'),
    ('Soy Allergy',       'ALLERGY',     'No soy'),
    ('Fish Allergy',      'ALLERGY',     'No fish'),
    ('Sesame Allergy',    'ALLERGY',     'No sesame'),
    ('Halal',             'RELIGIOUS',   'Halal foods only'),
    ('Kosher',            'RELIGIOUS',   'Kosher foods only')
on conflict (name) do nothing;

update dietary_restrictions set required_diet_flag = 'VEGAN'      where name = 'Vegan';
update dietary_restrictions set required_diet_flag = 'VEGETARIAN' where name = 'Vegetarian';
update dietary_restrictions set required_diet_flag = 'HALAL'      where name = 'Halal';
update dietary_restrictions set required_diet_flag = 'KOSHER'     where name = 'Kosher';

insert into restriction_excluded_allergens (restriction_id, allergen)
select r.id, x.allergen
from dietary_restrictions r
join (values
    ('Gluten-Free',       'GLUTEN'),
    ('Lactose-Free',      'MILK'),
    ('Nut Allergy',       'PEANUTS'),
    ('Nut Allergy',       'TREE_NUTS'),
    ('Shellfish Allergy', 'CRUSTACEANS'),
    ('Shellfish Allergy', 'MOLLUSCS'),
    ('Egg Allergy',       'EGGS'),
    ('Soy Allergy',       'SOY'),
    ('Fish Allergy',      'FISH'),
    ('Sesame Allergy',    'SESAME')
) as x (restriction, allergen) on x.restriction = r.name
on conflict do nothing;

-- ── Health conditions (BR-SAF-06, BR-PRF-07) ─────────────────────────────────
insert into health_conditions (name, description) values
    ('Diabetes',            'Type 1 or Type 2 diabetes'),
    ('High Cholesterol',    'Elevated LDL levels'),
    ('Hypertension',        'High blood pressure'),
    ('Celiac Disease',      'Gluten intolerance'),
    ('Lactose Intolerance', 'Dairy sugar intolerance')
on conflict (name) do nothing;

update health_conditions set sugar_limit_per_meal_g = 15   where name = 'Diabetes';
update health_conditions set fat_limit_per_meal_g = 25     where name = 'High Cholesterol';
update health_conditions set sodium_limit_per_day_mg = 2000 where name = 'Hypertension';

insert into condition_implied_allergens (condition_id, allergen)
select c.id, x.allergen
from health_conditions c
join (values
    ('Celiac Disease',      'GLUTEN'),
    ('Lactose Intolerance', 'MILK')
) as x (condition, allergen) on x.condition = c.name
on conflict do nothing;

-- ── Achievements (descriptions are corrected in V7 with US6) ─────────────────
insert into achievements (name, description, icon_name, type, threshold) values
    ('First Steps',      'Generated your first diet plan',   'seedling',  'PLANS_GENERATED', 1),
    ('Planner',          'Generated 5 diet plans',           'clipboard', 'PLANS_GENERATED', 5),
    ('Week Warrior',     'Logged progress 7 days in a row',  'fire',      'LOGIN_STREAK',    7),
    ('Consistent',       'Logged progress 30 days in a row', 'star',      'LOGIN_STREAK',    30),
    ('Hydration Hero',   'Drank 2L water for 7 days',        'droplet',   'HYDRATION',       7),
    ('Calorie Champion', 'Hit calorie goal 10 days',         'trophy',    'CALORIE_GOAL',    10)
on conflict (name) do nothing;
