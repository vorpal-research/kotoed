/* foreign keys */
create index if not exists submission_project_id_idx on submission(project_id);
create index if not exists project_course_id_idx on project(course_id);
create index if not exists project_denizen_id_idx on project(denizen_id);
create index if not exists submission_tag_submission_id_idx on submission_tag(submission_id);
create index if not exists submission_tag_tag_id_idx on submission_tag(tag_id);
create index if not exists submission_comment_submission_id_idx on submission_comment(submission_id);

/* sorting */
create index if not exists project_id_asc_index on project(id asc);
create index if not exists submission_id_asc_index on submission(id asc);
create index if not exists submission_tag_id_asc_index on submission_tag(id asc);
create index if not exists submission_datetime_desc_index on submission(datetime desc);
