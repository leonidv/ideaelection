--liquibase formatted sql
--changeset leonidv:0002

create table email_statuses
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into email_statuses (name, description)
VALUES ('planned', 'Email is planned to send'),
       ('sent', 'Email was sent to user'),
       ('error', 'The attempt to send finished with error. Check a reply_code for details');

create table emails (
    id  uuid primary key DEFAULT gen_random_uuid(),
    ctime timestampz not null default now(),
    utime timestampz not null default now(),
    address varchar(320) not null,
    subject varchar(100) not null,
    message varchar(2000) not null,
    status text references email_statuses default 'planned'
);