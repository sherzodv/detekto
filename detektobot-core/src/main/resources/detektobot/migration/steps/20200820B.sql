create table pack (
  code           varchar primary key,
  received_at    timestamp without time zone default (now() at time zone 'utc')
);

create table block (
  code           varchar primary key,
  pack_code      varchar not null,
  received_at    timestamp without time zone default (now() at time zone 'utc')
);
