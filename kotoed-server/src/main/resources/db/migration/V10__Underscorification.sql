ALTER TABLE build
  RENAME COLUMN submissionid TO submission_id;

ALTER TABLE build
  RENAME COLUMN tcbuildid TO tc_build_id;

ALTER TABLE course
  RENAME COLUMN buildtemplateid TO build_template_id;

ALTER TABLE denizen
  RENAME COLUMN denizenid TO denizen_id;

ALTER TABLE denizenrole
  RENAME COLUMN denizenid TO denizen_id;

ALTER TABLE denizenrole
  RENAME COLUMN roleid TO role_id;

ALTER TABLE denizenrole
  RENAME TO denizen_role;

ALTER TABLE junitstatistics
  RENAME COLUMN buildid TO build_id;

ALTER TABLE junitstatistics
  RENAME COLUMN artifactname TO artifact_name;

ALTER TABLE junitstatistics
  RENAME COLUMN artifactbody TO artifact_body;

ALTER TABLE junitstatistics
  RENAME COLUMN totaltestcount TO total_test_count;

ALTER TABLE junitstatistics
  RENAME COLUMN failedtestcount TO failed_test_count;

ALTER TABLE junitstatistics
  RENAME TO junit_statistics;

ALTER TABLE project
  RENAME COLUMN denizenid TO denizen_id;

ALTER TABLE project
  RENAME COLUMN courseid TO course_id;

ALTER TABLE project
  RENAME COLUMN repotype TO repo_type;

ALTER TABLE project
  RENAME COLUMN repourl TO repo_url;

ALTER TABLE rolepermission
  RENAME COLUMN roleid TO role_id;

ALTER TABLE rolepermission
  RENAME COLUMN permissionid TO permission_id;

ALTER TABLE rolepermission
  RENAME TO role_permission;

ALTER TABLE submission
  RENAME COLUMN parentsubmissionid TO parent_submission_id;

ALTER TABLE submission
  RENAME COLUMN projectid TO project_id;

ALTER TABLE submissioncomment
  RENAME COLUMN submissionid TO submission_id;

ALTER TABLE submissioncomment
  RENAME COLUMN authorid TO author_id;

ALTER TABLE submissioncomment
  RENAME TO submission_comment;

ALTER SEQUENCE denizenrole_id_seq
RENAME TO denizen_role_id_seq;

ALTER SEQUENCE junitstatistics_id_seq
RENAME TO junit_statistics_id_seq;

ALTER SEQUENCE rolepermission_id_seq
RENAME TO role_permission_id_seq;

ALTER SEQUENCE submissioncomment_id_seq
RENAME TO submission_comment_id_seq;
