CREATE TABLE course_status (
  id        SERIAL                NOT NULL PRIMARY KEY,
  course_id INT REFERENCES course NOT NULL,
  data      JSONB
);

CREATE TABLE project_status (
  id         SERIAL                 NOT NULL PRIMARY KEY,
  project_id INT REFERENCES project NOT NULL,
  data       JSONB
);

CREATE TABLE submission_status (
  id            SERIAL                    NOT NULL PRIMARY KEY,
  submission_id INT REFERENCES submission NOT NULL,
  data          JSONB
);
