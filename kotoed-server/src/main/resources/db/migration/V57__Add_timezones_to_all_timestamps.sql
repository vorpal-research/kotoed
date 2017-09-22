create function create_submission_comment_text_view() returns void
LANGUAGE SQL
AS $$
DROP VIEW IF EXISTS submission_comment_text_search CASCADE;

CREATE VIEW submission_comment_text_search AS
  SELECT
    submission_comment.*,
    setweight(to_tsvector('simple', author.denizen_id), 'A') ||
    setweight(to_tsvector('russian', author.denizen_id), 'A') ||
    setweight(to_tsvector('simple', owner.denizen_id), 'A') ||
    setweight(to_tsvector('russian', owner.denizen_id), 'A') ||
    setweight(to_tsvector('russian', sourcefile), 'A') ||
    setweight(to_tsvector('russian', regexp_replace(sourcefile, '.*[/\\]', '')), 'B') ||
    setweight(to_tsvector('russian', text), 'A') as document
  FROM submission_comment
    JOIN denizen_unsafe author ON submission_comment.author_id = author.id
    JOIN submission sub ON submission_comment.original_submission_id = sub.id
    JOIN project project ON sub.project_id = project.id
    JOIN denizen_unsafe owner ON project.denizen_id = owner.id;
$$;

DROP VIEW IF EXISTS submission_text_search CASCADE;

ALTER TABLE submission
  ALTER COLUMN datetime TYPE TIMESTAMP WITH TIME ZONE
  USING datetime::TIMESTAMP WITH TIME ZONE;

select create_submission_text_view();

DROP VIEW IF EXISTS submission_comment_text_search;

ALTER TABLE submission_comment
  ALTER COLUMN datetime TYPE TIMESTAMP WITH TIME ZONE
  USING datetime::TIMESTAMP WITH TIME ZONE;

select create_submission_comment_text_view();

ALTER TABLE submission_result
  ALTER COLUMN time TYPE TIMESTAMP WITH TIME ZONE
  USING time::TIMESTAMP WITH TIME ZONE;
