CREATE TYPE diff_mode_preference AS ENUM ('PREVIOUS_CLOSED', 'PREVIOUS_CHECKED', 'COURSE_BASE');
ALTER TABLE profile ADD COLUMN diff_mode_preference diff_mode_preference NOT NULL DEFAULT 'PREVIOUS_CLOSED';
