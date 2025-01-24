-- create_keybr_tables.sql

-- Switch to the database, if you want to be sure you're in the right one:
-- \c keybr_db;

-- Create a table to record daily practice minutes.
-- We'll store:
-- 1) An auto-incrementing primary key
-- 2) A date or timestamp of the day
-- 3) The total minutes practiced on that day

CREATE TABLE IF NOT EXISTS key_br_practice_record (
    id SERIAL PRIMARY KEY,                -- auto-incrementing primary key
    practice_date DATE NOT NULL UNIQUE,          -- the day you practiced (date only)
    minutes_practiced INT NOT NULL       -- total minutes practiced that day
);
-- Example additional constraints or indexes (if you need them):
-- CREATE UNIQUE INDEX ON practice_record (practice_date);
-- This ensures only one record per day, for instance, if that's desired.

-- You could also store a full timestamp (practice_timestamp) if you want
-- the exact time, e.g.:
--   practice_timestamp TIMESTAMP NOT NULL,
-- instead of or in addition to practice_date.
