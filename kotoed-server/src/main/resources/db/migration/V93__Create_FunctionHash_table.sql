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
    denizen_id INT REFERENCES denizen_unsafe NOT NULL,
    hash INT NOT NULL,
    level INT NOT NULL
);

CREATE INDEX IF NOT EXISTS function_part_hash_key ON function_part_hash(hash);
CREATE INDEX IF NOT EXISTS function_part_denizenId_key ON function_part_hash(denizen_id);

