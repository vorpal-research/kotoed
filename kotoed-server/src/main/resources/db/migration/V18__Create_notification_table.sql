CREATE TABLE notification (
  id SERIAL NOT NULL PRIMARY KEY,
  type TEXT,
  time TIMESTAMP DEFAULT now(),
  denizen_id INT REFERENCES denizen,
  title TEXT,
  body JSONB
);
