
-- we can't really reliably update version field, so just fuck the whole table
DELETE FROM web_session;

ALTER TABLE web_session ADD COLUMN version INT NOT NULL DEFAULT 0;
