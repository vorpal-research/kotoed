alter table project add column if not exists empty boolean default false;;
alter table project add column if not exists document tsvector default null;;
update project set (document, empty) = (select document, empty from project_text_search_backing where id = project.id)
where project.document is null;;
