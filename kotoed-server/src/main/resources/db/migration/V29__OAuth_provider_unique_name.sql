DELETE FROM oauth_provider op WHERE EXISTS(
    SELECT * FROM oauth_provider op2 WHERE op.id > op2.id AND op.name = op2.name
);

ALTER TABLE oauth_provider
  ADD UNIQUE (name);
