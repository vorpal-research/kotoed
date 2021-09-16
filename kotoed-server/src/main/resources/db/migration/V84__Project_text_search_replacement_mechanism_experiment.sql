drop function if exists create_project_text_backing_view();

create function create_project_text_backing_view() returns VOID
    LANGUAGE SQL
AS
$$
DROP VIEW IF EXISTS project_text_search_backing CASCADE;

    CREATE VIEW project_text_search_backing AS
    SELECT project.*,
           (select count(*) = 0 from submission s where s.project_id = project.id) as empty,
           setweight(to_tsvector('simple', owner.denizen_id), 'A') ||
           setweight(to_tsvector('russian', owner.denizen_id), 'A') ||
           setweight(to_tsvector('russian', coalesce(owner.email, '')), 'B') ||
           setweight(to_tsvector('russian', coalesce(profile.first_name, '')), 'A') ||
           setweight(to_tsvector('russian', coalesce(profile.last_name, '')), 'A') ||
           setweight(to_tsvector('simple', coalesce(profile.group_id, '')), 'A') ||
           setweight(to_tsvector('russian', coalesce(project.name, '')), 'A') ||
           setweight(to_tsvector('russian', coalesce(project.repo_url, '')), 'A') ||
           setweight(to_tsvector('russian', regexp_replace(project.repo_url, '[/\\]', ' ', 'g')), 'B') ||
           setweight(to_tsvector('russian', coalesce(course.name, '')), 'B') ||
           setweight(to_tsvector('simple', array_to_string(
                   array(
                           select tag.name
                           from submission_tag
                                    join tag on submission_tag.tag_id = tag.id
                                    join submission on submission_tag.submission_id = submission.id
                           where submission.project_id = project.id
                             and submission.state = 'open'
                       ), ' ')), 'B')                                              as document
    FROM project
             JOIN denizen_unsafe owner ON project.denizen_id = owner.id
             LEFT OUTER JOIN profile profile ON profile.denizen_id = owner.id
             JOIN course course ON project.course_id = course.id;
    $$;

SELECT create_project_text_backing_view();

drop table if exists project_text_search_supplement;
create table if not exists project_text_search_supplement
(
    id         serial   not null primary key,
    project_id int      not null unique references project,
    document   tsvector not null
);

insert into project_text_search_supplement(project_id, document)
select id, document
from project_text_search_backing;

create or replace function pts_make_trigger_project() returns trigger as
$$
begin
    insert into project_text_search_supplement (project_id, document)
    values (new.id, (select document from project_text_search_backing where id = new.id))
    on conflict(project_id) do update set document = (select document from project_text_search_backing where id = new.id);
    return NULL;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_project on project;
create trigger pts_trigger_project
    after insert or update or delete
    on project
    for each row
execute function pts_make_trigger_project();

create or replace function pts_make_trigger_submission() returns trigger as
$$
begin
    insert into project_text_search_supplement (project_id, document)
    values (new.project_id, (select document from project_text_search_backing where id = new.project_id))
    on conflict(project_id) do update set document = (select document
                                                      from project_text_search_backing
                                                      where id = new.project_id);
    return NULL;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_submission on submission;
create trigger pts_trigger_submission
    after insert or update or delete
    on submission
    for each row
execute function pts_make_trigger_submission();

create or replace function pts_make_trigger_submission_tag() returns trigger as
$$
declare
    proj_id integer;
begin
    select project_id into proj_id from submission s where new.submission_id = id;
    insert into project_text_search_supplement (project_id, document)
    values (proj_id, (select document from project_text_search_backing where id = proj_id))
    on conflict(project_id) do update set document = (select document from project_text_search_backing where id = proj_id);
    return NULL;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_submission_tag on submission_tag;
create trigger pts_trigger_submission_tag
    after insert or update or delete
    on submission_tag
    for each row
execute function pts_make_trigger_submission_tag();
