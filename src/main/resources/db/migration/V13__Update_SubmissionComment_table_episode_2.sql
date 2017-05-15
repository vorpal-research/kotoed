ALTER TABLE submission_comment ADD COLUMN previous_comment_id INT REFERENCES submission_comment;
