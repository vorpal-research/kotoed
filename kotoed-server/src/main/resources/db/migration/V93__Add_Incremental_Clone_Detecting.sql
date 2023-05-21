CREATE TABLE function
(
    id   SERIAL              NOT NULL PRIMARY KEY,
    name varchar(100) UNIQUE NOT NULL
);
CREATE TABLE function_part_hash
(
    id         SERIAL                  NOT NULL PRIMARY KEY,
    functionId   INT REFERENCES function   NOT NULL,
    submissionId INT REFERENCES submission NOT NULL,
    projectId INT REFERENCES project NOT NULL,
    leftBound integer not null,
    rightBound integer not null,
    hash int8 NOT NULL
);

CREATE INDEX IF NOT EXISTS function_part_hash_key ON function_part_hash(hash);
CREATE INDEX IF NOT EXISTS function_part_denizenId_key ON function_part_hash(projectId);

CREATE TABLE processed_project_sub
(
    id         SERIAL                  NOT NULL PRIMARY KEY,
    projectId INT REFERENCES project NOT NULL,
    submissionId INT REFERENCES submission NOT NULL
);
create table function_leaves
(
    id         SERIAL                  NOT NULL PRIMARY KEY,
    functionId   INT REFERENCES function   NOT NULL,
    submissionId INT REFERENCES submission NOT NULL,
    leavesCount INT NOT NULL
);
CREATE TABLE hash_clones(
                            id bigserial primary key,
                            f_functionId INT REFERENCES function   NOT NULL,
                            f_submissionId INT REFERENCES submission NOT NULL,
                            f_projectId INT REFERENCES project NOT NULL,
                            f_leftBound INT NOT NULL,
                            f_rightBound INT NOT NULL,
                            s_functionId INT REFERENCES function   NOT NULL,
                            s_submissionId INT REFERENCES submission NOT NULL,
                            s_projectId INT REFERENCES project NOT NULL,
                            s_leftBound INT NOT NULL,
                            s_rightBound INT NOT NULL
)
