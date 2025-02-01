CREATE TABLE load_cell_session (
    id SERIAL PRIMARY KEY,                  -- Auto-incrementing primary key
    date DATE NOT NULL,                     -- Date of the exercise session
    start_time TIMESTAMP NOT NULL,          -- Start time of the exercise
    end_time TIMESTAMP NOT NULL,            -- End time of the exercise
    duration_seconds BIGINT NOT NULL        -- Duration of the exercise in seconds
);