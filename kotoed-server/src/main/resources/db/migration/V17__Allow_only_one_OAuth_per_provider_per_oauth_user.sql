DELETE FROM oauth_profile op WHERE EXISTS(
    SELECT * FROM oauth_profile op2 WHERE op.id > op2.id AND op.oauth_user_id = op2.oauth_user_id AND op.oauth_provider_id = op2.oauth_provider_id
);

ALTER TABLE oauth_profile
  ADD UNIQUE (oauth_user_id, oauth_provider_id);
