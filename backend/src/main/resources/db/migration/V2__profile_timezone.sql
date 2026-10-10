-- V2: user timezone (BR-TIM-01). "Today" for targets, plans and progress is the user's calendar day.
alter table user_profiles add column timezone varchar(64) not null default 'UTC';
