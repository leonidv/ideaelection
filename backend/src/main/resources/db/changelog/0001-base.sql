--liquibase formatted sql
--changeset leonidv:0001

CREATE COLLATION case_insensitive (provider = icu, locale = 'und-u-ks-level2', deterministic = false);

------- USERS --------

create table subscription_plans
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into subscription_plans (name, description)
VALUES ('free', 'Plan without any subscription. 10 members in private groups'),
       ('basic', 'Plan with 10 persons in private group and 1 domain restrictions'),
       ('enterprise', 'No any restrictions');

create table users
(
    id                uuid primary key DEFAULT gen_random_uuid(),
    ctime             timestamp with time zone not null  DEFAULT now(),
    external_id       text                     not null,
    email             text                     not null,
    display_name      text                     not null,
    avatar            text,
    super_admin       boolean                  not null  default false,
    subscription_plan text references subscription_plans default 'free'
);

create unique index users_external_id on users (external_id collate case_insensitive);
create unique index users_email on users (email collate case_insensitive);



create table user_notification_frequency
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into user_notification_frequency(name, description)
values ('instantly', 'notification will be send after creation'),
       ('daily', 'send one notification per day'),
       ('weekly', 'send one notification per weekly'),
       ('disabled', 'dont send any notifications');

create table user_settings
(
    user_id                uuid REFERENCES users ON DELETE cascade,
    notification_frequency text REFERENCES user_notification_frequency not null,
    subscribed_to_news     boolean                                     not null
);

----------- GROUPS ---------

create table group_entry_modes
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into group_entry_modes(name, description)
VALUES ('PUBLIC', 'Visible in the list of available groups. JoinRequest is not required'),
       ('CLOSED', 'Visible in the list of available groups. JoinRequest is required'),
       ('PRIVATE', 'Not visible in the list of available groups. JoinRequest is required');

create table group_states
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into group_states(name, description)
VALUES ('ACTIVE', 'Group is available for working'),
       ('DELETED', 'Group is logically deleted');

create table groups
(
    id                  uuid primary key DEFAULT gen_random_uuid(),
    joining_key         text         not null,
    ctime               timestamp with time zone default now(),
    state               text references group_states,
    creator             uuid references users,
    name                varchar(255) not null,
    description         varchar(300) not null,
    logo                text         not null,
    entry_mode          text references group_entry_modes,
    entry_question      varchar(200),
    domain_restrictions text array   not null
);



create table group_member_roles
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into group_member_roles(name, description)
VALUES ('group_admin', 'Administrator of group. Can edit settings, accept join requests, etc.'),
       ('member', 'Member of group. Can works with group''s ideas, votes for it, etc.');

create table group_members
(
    user_id       uuid references users,
    group_id      uuid references groups,
    ctime         timestamp with time zone default now(),
    role_in_group text references group_member_roles,
    primary key (user_id, group_id)
);

create index group_members__group_id_idx on group_members (group_id);
create index group_members__user_id_idx on group_members (user_id);

-------- IDEAS ----------

create table ideas
(
    id                     uuid primary key DEFAULT gen_random_uuid(),
    ctime                  timestamp with time zone not null default now(),
    group_id               uuid references groups,
    summary                varchar(255) check ( length(summary) > 3 ),
    description            varchar(10000)           not null,
    description_plain_text varchar(2000)            not null,
    link                   varchar(1000)            not null,
    assignee               uuid references users,
    implemented            boolean                  not null,
    author                 uuid references users    not null,
    voters                 uuid array               not null,
    archived               boolean                  not null,
    deleted                boolean                  not null
);

create index ideas_group_id_idx on ideas (group_id);

-------- INVITES -----------

create table invite_accept_statuses
(
    name        text primary key,
    description text not null
);

insert into invite_accept_statuses (name, description)
VALUES ('unresolved', 'Invite is still waiting for resolution'),
       ('approved', 'Invite was approved and group members is created'),
       ('declined', 'Invite was rejected'),
       ('invoked','Invite was revoked by group''s admin');

create table invite_email_statuses
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into invite_email_statuses (name, description)
VALUES ('wait_to_sent', 'Email is waiting to be sent'),
       ('sent', 'Email has been sent');

create table invites
(
    id           uuid primary key DEFAULT gen_random_uuid(),
    ctime        timestamptz  not null default now(),
    mtime        timestamptz  not null default now(),
    user_id      uuid references users,
    group_id     uuid         not null references groups,
    status       text         not null references invite_accept_statuses,
    author       uuid         not null references users,
    user_email   varchar(200) not null,
    email_status text         not null references invite_email_statuses,
    message      varchar(200) not null default ''
);

create unique index invites_group_id_user_id_unq_idx on invites (user_id, group_id) where (invites.status = 'unresolved');
create index invites_user_id_idx on invites (user_id);
create index invites_group_id_idx on invites (group_id);
create index invites_emails on invites (user_email);
create index invites_email_status on invites (email_status);



-------- JOIN REQUEST --------

create table joinrequest_accept_statuses
(
    name        text collate case_insensitive primary key,
    description text not null
);

insert into joinrequest_accept_statuses (name, description)
VALUES ('unresolved', 'Joinrequest  is still waiting for resolution'),
       ('approved', 'Joinrequest was approved and group members is created'),
       ('declined', 'Joinrequest was canceled by group''s admin'),
       ('revoked', 'Joinrequest was revoked by author');


create table joinrequests
(
    id       uuid primary key DEFAULT gen_random_uuid(),
    ctime    timestamptz not null DEFAULT now(),
    mtime    timestamptz not null DEFAULT now(),
    user_id  uuid        not null references users,
    group_id uuid        not null references groups,
    status   text        not null references joinrequest_accept_statuses,
    message  varchar(200)
);

create index joinrequests_user_id_idx on joinrequests (user_id);
create index joinrequests_group_id_idx on joinrequests (group_id);
create unique index joinrequests_group_id_user_id_unq_idx on joinrequests (user_id, group_id) where (joinrequests.status = 'unresolved');