ALTER TABLE course ADD COLUMN IF NOT EXISTS base_repo_url TEXT DEFAULT NULL;
ALTER TABLE course ADD COLUMN IF NOT EXISTS base_revision TEXT DEFAULT '';

create or replace function create_course_text_view() returns VOID
LANGUAGE SQL
AS $$
DROP VIEW IF EXISTS course_text_search CASCADE;

CREATE VIEW course_text_search AS
  SELECT
    course.*,
    setweight(to_tsvector('russian', coalesce(course.base_repo_url, '')), 'A') ||
    setweight(to_tsvector('russian', regexp_replace(coalesce(course.base_repo_url, ''), '[/\\]', ' ', 'g')), 'B') ||
    setweight(to_tsvector('simple', coalesce(course.name, '')), 'A') ||
    setweight(to_tsvector('russian', coalesce(course.name, '')), 'A')
      as document

  FROM course;
$$;

SELECT create_course_text_view();
