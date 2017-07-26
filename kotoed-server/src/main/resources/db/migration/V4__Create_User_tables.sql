CREATE TABLE Denizen (
  id        SERIAL NOT NULL PRIMARY KEY,
  denizenId TEXT UNIQUE,
  password  TEXT,
  salt      TEXT
);

CREATE TABLE Course (
  id              SERIAL NOT NULL PRIMARY KEY,
  name            TEXT,
  buildTemplateId TEXT
);

CREATE TABLE Project (
  id        SERIAL NOT NULL PRIMARY KEY,
  denizenId INT REFERENCES Denizen,
  courseId  INT REFERENCES Course,
  repoType  TEXT,
  repoUrl   TEXT
);

CREATE TYPE SUBMISSIONSTATE
AS ENUM ('open', 'obsolete', 'closed');

CREATE TABLE Submission (
  id                 SERIAL NOT NULL PRIMARY KEY,
  datetime           TIMESTAMP DEFAULT current_timestamp,
  parentSubmissionId INT REFERENCES Submission,
  projectId          INT REFERENCES Project,
  state              SUBMISSIONSTATE,
  revision           TEXT
);

CREATE TABLE Build (
  id           SERIAL NOT NULL PRIMARY KEY,
  submissionId INT REFERENCES Submission,
  tcBuildId    INT
);

CREATE TYPE SUBMISSIONCOMMENTSTATE
AS ENUM ('open', 'closed');

CREATE TABLE SubmissionComment (
  id           SERIAL NOT NULL PRIMARY KEY,
  datetime     TIMESTAMP DEFAULT current_timestamp,
  submissionId INT REFERENCES Submission,
  authorId     INT REFERENCES Denizen,
  state        SUBMISSIONCOMMENTSTATE,
  sourceFile   TEXT,
  sourceLine   INT,
  text         TEXT
);

CREATE TABLE Role (
  id   SERIAL NOT NULL PRIMARY KEY,
  name TEXT
);

CREATE TABLE Permission (
  id   SERIAL NOT NULL PRIMARY KEY,
  name TEXT
);

CREATE TABLE RolePermission (
  id           SERIAL NOT NULL PRIMARY KEY,
  roleId       INT REFERENCES Role,
  permissionId INT REFERENCES Permission
);

CREATE TABLE DenizenRole (
  id        SERIAL NOT NULL PRIMARY KEY,
  denizenId INT REFERENCES Denizen,
  roleId    INT REFERENCES Role
);
