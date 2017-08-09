CREATE TABLE submission_result (
  id SERIAL NOT NULL PRIMARY KEY,
  time TIMESTAMP DEFAULT now(),
  type TEXT NOT NULL,
  submission_id INT NOT NULL REFERENCES submission,
  body JSONB
);
