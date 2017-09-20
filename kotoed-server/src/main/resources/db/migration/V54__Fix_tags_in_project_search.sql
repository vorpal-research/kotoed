create or replace function create_project_text_view() returns VOID
LANGUAGE SQL
AS $$
DROP VIEW IF EXISTS project_text_search CASCADE;

CREATE VIEW project_text_search AS
  SELECT
    project.*,
    setweight(to_tsvector('simple', owner.denizen_id), 'A') ||
    setweight(to_tsvector('russian', owner.denizen_id), 'A') ||
    setweight(to_tsvector('russian', coalesce(owner.email, '')), 'B') ||
    setweight(to_tsvector('russian', coalesce(profile.first_name,'')), 'A') ||
    setweight(to_tsvector('russian', coalesce(profile.last_name,'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(profile.group_id,'')), 'A') ||
    setweight(to_tsvector('russian', coalesce(project.name, '')), 'A') ||
    setweight(to_tsvector('russian', coalesce(project.repo_url, '')), 'A') ||
    setweight(to_tsvector('russian', regexp_replace(project.repo_url, '[/\\]', ' ', 'g')), 'B') ||
    setweight(to_tsvector('russian', coalesce(course.name, '')), 'B') ||
    setweight(to_tsvector('simple', array_to_string(
        array(
            select tag.name from submission_tag
              join tag on submission_tag.tag_id = tag.id
              join submission on submission_tag.submission_id = submission.id
            where submission.project_id = project.id and submission.state = 'open'
        ), ' ')), 'B') as document
  FROM project
    JOIN denizen_unsafe owner ON project.denizen_id = owner.id
    LEFT OUTER JOIN profile profile ON profile.denizen_id = owner.id
    JOIN course course ON project.course_id = course.id;
$$;

SELECT create_project_text_view();
SELECT create_submission_text_view();
