delete from project_text_search_supplement where true;

insert into project_text_search_supplement(project_id, document)
select id, document
from project_text_search_backing;

create or replace function pts_make_trigger_submission_tag() returns trigger as
$$
declare
    proj_id integer;
    doc tsvector;
begin
    select project_id into proj_id from submission s where old.submission_id = id or new.submission_id = id;
    select document into doc from project_text_search_backing where id = proj_id;
    insert into project_text_search_supplement (project_id, document)
    values (proj_id, doc)
    on conflict(project_id) do update set document = doc;
    return NULL;
end;
$$ LANGUAGE plpgsql;;

drop trigger if exists pts_trigger_submission_tag on submission_tag;
create trigger pts_trigger_submission_tag
    after insert or update or delete
    on submission_tag
    for each row
execute function pts_make_trigger_submission_tag();
