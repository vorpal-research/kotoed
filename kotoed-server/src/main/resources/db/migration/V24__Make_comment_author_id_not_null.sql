DELETE FROM submission_comment WHERE author_id IS NULL;
ALTER TABLE submission_comment ALTER COLUMN author_id SET NOT NULL;
