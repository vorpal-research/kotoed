CREATE TABLE build_template (
  id SERIAL NOT NULL PRIMARY KEY,
  command_line JSONB NOT NULL
);

INSERT INTO build_template (id, command_line) VALUES (1, '[
    { "type" : "SHELL", "command_line": ["git", "init"] },
    { "type" : "SHELL", "command_line": ["git", "fetch", "$REPO"] },
		{ "type" : "SHELL", "command_line": ["git", "reset", "--hard", "$REVISION"] },
		{ "type" : "SHELL", "command_line": ["git", "clone", "--depth", "1", "https://bitbucket.org/kotlinpolytech/kfirst-tests", "random-tests"] },
		{ "type" : "SHELL", "command_line": ["rsync", "-rtv", "--ignore-existing", "random-tests/test/", "test/"] },
		{ "type" : "SHELL", "command_line": ["rsync", "random-tests/pom.xml", "pom.xml"] },
		{ "type" : "SHELL", "command_line": ["mvn", "clean", "test-compile", "kfirst-runner:run"] }
]'::jsonb);

ALTER TABLE course ADD COLUMN IF NOT EXISTS
  build_template_id INT NOT NULL REFERENCES build_template DEFAULT 1;
