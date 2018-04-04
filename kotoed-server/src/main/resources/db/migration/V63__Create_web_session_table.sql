CREATE TABLE web_session (
  id TEXT NOT NULL PRIMARY KEY,
  last_accessed BIGINT NOT NULL,
  timeout BIGINT NOT NULL,
  data TEXT NOT NULL
);
