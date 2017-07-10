ALTER TABLE submission_comment ALTER COLUMN submission_id SET NOT NULL;

ALTER TABLE submission_comment ADD COLUMN original_submission_id INT REFERENCES submission;
UPDATE submission_comment SET original_submission_id = submission_id;
ALTER TABLE submission_comment ALTER COLUMN original_submission_id SET NOT NULL;

ALTER TABLE submission_comment ADD COLUMN persistent_comment_id SERIAL;
