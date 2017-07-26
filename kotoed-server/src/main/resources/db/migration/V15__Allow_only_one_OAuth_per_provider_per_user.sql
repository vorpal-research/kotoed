ALTER TABLE oauth_profile
  ADD UNIQUE (denizen_id, oauth_provider_id);
