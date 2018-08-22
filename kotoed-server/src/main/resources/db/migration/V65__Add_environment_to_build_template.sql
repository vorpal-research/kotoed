
ALTER TABLE build_template ADD COLUMN IF NOT EXISTS
  environment JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE build_template SET environment = '{
  "MAVEN_OPTS" : "-Xmx512M",
  "GIT_SSH_COMMAND" : "ssh -i id_rsa.kotoed",
  "GIT_ASKPASS" : "true"
}'::jsonb WHERE id =  1
