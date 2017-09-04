CREATE TABLE profile(
  id SERIAL NOT NULL PRIMARY KEY,
  denizen_id INT REFERENCES denizen_unsafe UNIQUE NOT NULL,
  first_name TEXT,
  last_name TEXT,
  group_id TEXT
);
