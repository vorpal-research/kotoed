
create or replace function pts_make_trigger_project() returns trigger as
$$
declare
    proj_id integer;
    proj_document tsvector;
    proj_empty boolean;
begin
    select new.id into proj_id;
    select document, empty into proj_document, proj_empty from project_text_search_backing where id = proj_id;
    new.document := proj_document;
    new.empty := proj_empty;
    return new;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_project on project;
create trigger pts_trigger_project
    before update
    on project
    for each row
execute function pts_make_trigger_project();

create or replace function pts_make_trigger_project_on_insert() returns trigger as
$$
begin
    update project set empty = DEFAULT where id = new.id;
    return null;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_project_on_insert on project;
create trigger pts_trigger_project_on_insert
    after insert
    on project
    for each row
execute function pts_make_trigger_project_on_insert();

create or replace function pts_make_trigger_submission() returns trigger as
$$
declare
    proj_id integer;
    proj_document tsvector;
    proj_empty boolean;
begin
    select new.project_id into proj_id;
    select document, empty into proj_document, proj_empty from project_text_search_backing where id = proj_id;
    update project set (document, empty) = (proj_document, proj_empty);
    return NULL;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_submission on submission;
create trigger pts_trigger_submission
    after insert or update
    on submission
    for each row
execute function pts_make_trigger_submission();

create or replace function pts_make_trigger_submission_tag() returns trigger as
$$
declare
    proj_id integer;
    proj_document tsvector;
    proj_empty boolean;
begin
    select project_id into proj_id from submission s where new.submission_id = id or old.submission_id = id;
    select document, empty into proj_document, proj_empty from project_text_search_backing where id = proj_id;
    update project set (document, empty) = (proj_document, proj_empty);
    return NULL;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_submission_tag on submission_tag;
create trigger pts_trigger_submission_tag
    after insert or update or delete
    on submission_tag
    for each row
execute function pts_make_trigger_submission_tag();

create or replace function create_project_text_view() returns void
    language sql
as
$$
    drop view if exists project_text_search cascade;
    create view project_text_search as select * from project;
$$;
select create_project_text_view();
select create_submission_text_view();

create index if not exists project_document_gin_idx on project using gin (document);
