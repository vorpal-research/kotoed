alter table tag add column suggest_for_search boolean not null default true;

update tag set suggest_for_search = false where name ~ '-?[0-9]+';
update tag set suggest_for_search = false where name in ('excellent', 'good', 'fair', 'mediocre', 'permanent');
