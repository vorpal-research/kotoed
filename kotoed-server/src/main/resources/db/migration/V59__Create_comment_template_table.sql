CREATE TABLE IF NOT EXISTS comment_template(
  id          SERIAL  NOT NULL PRIMARY KEY,
  denizen_id  INT     NOT NULL REFERENCES denizen_unsafe,
  name        TEXT    NOT NULL UNIQUE,
  text        TEXT    NOT NULL
);
