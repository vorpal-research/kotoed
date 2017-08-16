CREATE VIEW project_text_search AS
  SELECT
    project.*,
    setweight(to_tsvector('simple', owner.denizen_id), 'A') ||
    setweight(to_tsvector('simple', coalesce(owner.email, '')), 'B') ||
    setweight(to_tsvector('russian', coalesce(project.name, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(project.repo_url, '')), 'A') ||
    setweight(to_tsvector('simple', regexp_replace(project.repo_url, '[/\\]', ' ', 'g')), 'B') ||
    setweight(to_tsvector('russian', coalesce(course.name, '')), 'B') as document
  FROM project
    JOIN denizen_unsafe owner ON project.denizen_id = owner.id
    JOIN course course ON project.course_id = course.id;
