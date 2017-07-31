UPDATE submission_comment SET state = 'open' WHERE state IS NULL;
ALTER TABLE submission_comment ALTER COLUMN state SET NOT NULL;
