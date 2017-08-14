CREATE VIEW submission_comment_text_search AS
  SELECT
    submission_comment.*,
    setweight(to_tsvector(author.denizen_id), 'A') ||
    setweight(to_tsvector(sourcefile), 'B') ||
    setweight(to_tsvector('russian', text), 'A') as document
  FROM submission_comment
    JOIN denizen_unsafe author ON submission_comment.author_id = author.id;