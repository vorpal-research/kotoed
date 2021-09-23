create or replace function pts_make_trigger_submission() returns trigger as
$$
declare
    proj_id integer;
begin
    select new.project_id into proj_id;
    update project set empty = DEFAULT where id = proj_id;
    return null;
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
begin
    select project_id into proj_id from submission s where new.submission_id = id or old.submission_id = id;
    update project set empty = DEFAULT where id = proj_id;
    return null;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_submission_tag on submission_tag;
create trigger pts_trigger_submission_tag
    after insert or update or delete
    on submission_tag
    for each row
execute function pts_make_trigger_submission_tag();
