create table dm_code_check_req (
  id             serial primary key,
  user_id        bigint,
  user_name      varchar,
  first_name     varchar,
  last_name      varchar,
  text           varchar,
  file_id        varchar,
  file_unique_id varchar,
  code           varchar,
  decode_error   varchar,
  received_at    timestamp without time zone default (now() at time zone 'utc')
);

create table command_req (
  id             serial primary key,
  user_id        bigint,
  user_name      varchar,
  first_name     varchar,
  last_name      varchar,
  text           varchar,
  received_at    timestamp without time zone default (now() at time zone 'utc')
);
