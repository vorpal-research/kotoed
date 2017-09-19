DROP VIEW IF EXISTS submission_text_search CASCADE;

CREATE VIEW submission_text_search AS
  SELECT
    submission.*,
    (
      project.document ||
      setweight(to_tsvector('simple', submission.revision), 'B') ||
      setweight(to_tsvector('simple', (submission.state)::text), 'C') ||
      setweight(to_tsvector('simple', array_to_string(
          array(
              select tag.name from submission_tag
                join tag on submission_tag.tag_id = tag.id
              where submission_tag.submission_id = submission.id
          ), ' ')), 'B')
    )
    as document
  FROM submission
    JOIN project_text_search project ON submission.project_id = project.id;
