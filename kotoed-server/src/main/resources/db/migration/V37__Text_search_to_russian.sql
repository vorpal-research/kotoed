DROP VIEW project_text_search;

CREATE VIEW project_text_search AS
  SELECT
    project.*,
    setweight(to_tsvector('simple', owner.denizen_id), 'A') ||
    setweight(to_tsvector('russian', owner.denizen_id), 'A') ||
    setweight(to_tsvector('russian', coalesce(owner.email, '')), 'B') ||
    setweight(to_tsvector('russian', coalesce(project.name, '')), 'A') ||
    setweight(to_tsvector('russian', coalesce(project.repo_url, '')), 'A') ||
    setweight(to_tsvector('russian', regexp_replace(project.repo_url, '[/\\]', ' ', 'g')), 'B') ||
    setweight(to_tsvector('russian', coalesce(course.name, '')), 'B') as document
  FROM project
    JOIN denizen_unsafe owner ON project.denizen_id = owner.id
    JOIN course course ON project.course_id = course.id;


DROP VIEW submission_comment_text_search;

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
