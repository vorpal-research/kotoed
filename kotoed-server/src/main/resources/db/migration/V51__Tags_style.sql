ALTER TABLE tag
    ADD COLUMN style JSONB DEFAULT '{}'::jsonb NOT NULL; -- Camel cased CSS
