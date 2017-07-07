CREATE TABLE oauth_provider (
  id   SERIAL NOT NULL PRIMARY KEY,
  name TEXT   NOT NULL
);

CREATE TABLE oauth_profile (
  id                SERIAL NOT NULL PRIMARY KEY,
  denizen_id        INT REFERENCES denizen,
  oauth_provider_id INT REFERENCES oauth_provider,
  oauth_user_id     TEXT   NOT NULL
);
