UPDATE course SET name = '' WHERE name IS NULL ;;
ALTER TABLE course ALTER COLUMN name SET NOT NULL ;;

DELETE FROM denizen_role WHERE denizen_id IS NULL OR role_id IS NULL ;;
ALTER TABLE denizen_role ALTER COLUMN denizen_id SET NOT NULL ;;
ALTER TABLE denizen_role ALTER COLUMN role_id SET NOT NULL ;;

ALTER TABLE denizen_unsafe ALTER COLUMN denizen_id SET NOT NULL ;;
ALTER TABLE denizen_unsafe ALTER COLUMN password SET NOT NULL ;;
ALTER TABLE denizen_unsafe ALTER COLUMN salt SET NOT NULL ;;

DELETE FROM notification WHERE denizen_id IS NULL OR type IS NULL OR title IS NULL ;;
ALTER TABLE notification ALTER COLUMN denizen_id SET NOT NULL ;;
ALTER TABLE notification ALTER COLUMN type SET NOT NULL ;;
ALTER TABLE notification ALTER COLUMN title SET NOT NULL ;;

ALTER TABLE oauth_profile ALTER COLUMN denizen_id SET NOT NULL ;;
ALTER TABLE oauth_profile ALTER COLUMN oauth_provider_id SET NOT NULL ;;

DELETE FROM project WHERE name IS NULL OR repo_type IS NULL OR repo_url IS NULL ;;
ALTER TABLE project ALTER COLUMN name SET NOT NULL ;;
ALTER TABLE project ALTER COLUMN repo_type SET NOT NULL ;;
ALTER TABLE project ALTER COLUMN repo_url SET NOT NULL ;;

DELETE FROM role_permission WHERE role_id IS NULL OR permission_id IS NULL ;;
ALTER TABLE role_permission ALTER COLUMN role_id SET NOT NULL ;;
ALTER TABLE role_permission ALTER COLUMN permission_id SET NOT NULL ;;

UPDATE submission_comment SET text = '' WHERE text IS NULL ;;
UPDATE submission_comment SET sourcefile = '' WHERE sourcefile IS NULL ;;
UPDATE submission_comment SET sourceline = 0 WHERE sourceline IS NULL ;;
ALTER TABLE submission_comment ALTER COLUMN text SET NOT NULL ;;
ALTER TABLE submission_comment ALTER COLUMN sourcefile SET NOT NULL ;;
ALTER TABLE submission_comment ALTER COLUMN sourceline SET NOT NULL ;;
