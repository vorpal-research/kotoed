CREATE VIEW denizen_text_search AS
  SELECT
    denizen.*,
    setweight(to_tsvector('simple', denizen.denizen_id), 'A') ||
    setweight(to_tsvector('russian', denizen.denizen_id), 'A') ||
    setweight(to_tsvector('russian', coalesce(denizen.email, '')), 'B') ||
    setweight(to_tsvector('russian', coalesce(profile.first_name, '')), 'A') ||
    setweight(to_tsvector('russian', coalesce(profile.last_name, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(profile.group_id, '')), 'B')
    as document
  FROM denizen
    LEFT OUTER JOIN profile ON denizen.id = profile.denizen_id;
