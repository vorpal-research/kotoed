CREATE VIEW course_text_search AS
  SELECT
    course.*,
    setweight(to_tsvector('simple', coalesce(course.name, '')), 'A') ||
    setweight(to_tsvector('russian', coalesce(course.name, '')), 'A')
      as document

  FROM course;