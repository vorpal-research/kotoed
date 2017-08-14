ALTER TABLE oauth_provider ADD COLUMN client_id TEXT DEFAULT NULL;
ALTER TABLE oauth_provider ADD COLUMN client_secret TEXT DEFAULT NULL;

UPDATE oauth_provider SET client_id = 'UNKNOWN' WHERE client_id IS NULL;
UPDATE oauth_provider SET client_secret = 'UNKNOWN' WHERE client_secret IS NULL;

ALTER TABLE oauth_provider ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE oauth_provider ALTER COLUMN client_secret SET NOT NULL;
