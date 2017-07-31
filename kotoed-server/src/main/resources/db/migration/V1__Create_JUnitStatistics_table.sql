CREATE TABLE JUnitStatistics (
  id              SERIAL NOT NULL PRIMARY KEY,
  buildId         INT    NOT NULL,
  artifactName    TEXT   NOT NULL,
  artifactBody    JSONB  NOT NULL,
  totalTestCount  INT    NOT NULL,
  failedTestCount INT    NOT NULL
);
