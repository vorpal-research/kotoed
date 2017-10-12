ALTER TABLE comment_template DROP CONSTRAINT comment_template_name_key;

ALTER TABLE comment_template
  ADD UNIQUE (denizen_id, name);
