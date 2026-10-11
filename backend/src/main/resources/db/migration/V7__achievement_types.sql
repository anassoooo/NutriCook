-- V7: fair achievements (BR-ACH-01..06). Every description states the exact rule.

alter table achievements drop constraint ck_achievements_type;

-- "Login" streaks were always counted from progress entries: name them for what they measure
update achievements set type = 'LOGGING_STREAK' where type = 'LOGIN_STREAK';

-- Protein intake is not tracked, so a protein goal cannot be measured (BR-ACH-05)
delete from user_achievements
where achievement_id in (select id from achievements where type = 'PROTEIN_GOAL');
delete from achievements where type = 'PROTEIN_GOAL';

alter table achievements add constraint ck_achievements_type
    check (type in ('LOGGING_STREAK', 'PLANS_GENERATED', 'CALORIE_GOAL', 'HYDRATION', 'WEIGHT_MILESTONE'));

update achievements set description = case name
    when 'First Steps'
        then 'Get a meal plan for 1 day. Regenerating the same day does not count again.'
    when 'Planner'
        then 'Get meal plans for 5 different days. Regenerating a day does not count again.'
    when 'Week Warrior'
        then 'Log your progress 7 days in a row. Until you log today, your streak runs through yesterday.'
    when 'Consistent'
        then 'Log your progress 30 days in a row. Until you log today, your streak runs through yesterday.'
    when 'Hydration Hero'
        then 'Drink at least 2,000 ml of water on 7 of the last 30 days.'
    when 'Calorie Champion'
        then 'Eat within 10% of the day''s calorie target on 10 of the last 30 days.'
    else description
end;

insert into achievements (name, description, icon_name, type, threshold) values
    ('On the Move',
     'Change your weight by 2 kg in your goal''s direction (losing or gaining) since your first logged weight.',
     'scale', 'WEIGHT_MILESTONE', 2),
    ('Transformation',
     'Change your weight by 5 kg in your goal''s direction (losing or gaining) since your first logged weight.',
     'medal', 'WEIGHT_MILESTONE', 5)
on conflict (name) do nothing;

-- The first weigh-in is looked up per user (BR-ACH-05)
create index ix_progress_user_weighed on progress_entries (user_id, date) where weight_kg is not null;
