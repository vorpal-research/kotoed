DROP VIEW submission_comment_text_search;

CREATE VIEW submission_comment_text_search AS
  SELECT
    submission_comment.*,
    setweight(to_tsvector('simple', author.denizen_id), 'A') ||
    setweight(to_tsvector('simple', owner.denizen_id), 'A') ||
    setweight(to_tsvector('simple', sourcefile), 'A') ||
    setweight(to_tsvector('simple', regexp_replace(sourcefile, '.*[/\\]', '')), 'B') ||
    setweight(to_tsvector('russian', text), 'A') as document
  FROM submission_comment
    JOIN denizen_unsafe author ON submission_comment.author_id = author.id
    JOIN submission sub ON submission_comment.original_submission_id = sub.id
    JOIN project project ON sub.project_id = project.id
    JOIN denizen_unsafe owner ON project.denizen_id = owner.id;
