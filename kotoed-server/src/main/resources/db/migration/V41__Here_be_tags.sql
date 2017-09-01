CREATE TABLE tag(
  id SERIAL NOT NULL PRIMARY KEY,
  name TEXT UNIQUE NOT NULL
);

CREATE TABLE submission_tag(
  id SERIAL NOT NULL PRIMARY KEY,
  submission_id INT REFERENCES submission NOT NULL,
  tag_id INT REFERENCES tag NOT NULL
);

ALTER TABLE submission_tag
  ADD UNIQUE(submission_id, tag_id);
