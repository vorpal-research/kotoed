CREATE TYPE course_state
AS ENUM ('open', 'frozen', 'closed');

ALTER TABLE course
  ADD COLUMN IF NOT EXISTS state course_state NOT NULL
  DEFAULT 'open';
