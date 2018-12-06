CREATE TABLE IF NOT EXISTS push_subscription (
  id                  SERIAL NOT NULL PRIMARY KEY,
  denizen_id          INT    NOT NULL REFERENCES denizen_unsafe,
  subscription_object jsonb  NOT NULL
);;
