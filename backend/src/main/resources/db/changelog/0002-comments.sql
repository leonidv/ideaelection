--liquibase formatted sql
--changeset leonidv:0002

drop table if exists comments;

create table comments
(
    id             uuid primary key         DEFAULT gen_random_uuid(),
    ctime          timestamp not null       default now(),
    idea_id        uuid      not null references ideas,
    author         uuid      not null references users,
    content        text      not null check ( char_length(content) between 1 and 1000 ),
    last_edit_time timestamp                default null,
    last_edit_by   uuid references users    default null,
    reply_to       uuid references comments default null,

    check (
            (last_edit_by IS NULL AND last_edit_time IS NULL) OR
            (last_edit_by IS NOT NULL AND last_edit_time IS NOT NULL)
        )
);

COMMENT ON table comments IS 'Comments of Idea from users';
COMMENT ON column comments.last_edit_time IS 'Time of last comment''s edit. If null, comment is not edited';
COMMENT ON column comments.reply_to IS 'This comment is answer to another comment'