CREATE TYPE notification_status AS ENUM ('unread', 'read');

ALTER TABLE notification ADD COLUMN status notification_status NOT NULL DEFAULT 'unread';
