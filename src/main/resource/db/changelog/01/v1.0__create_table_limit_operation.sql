CREATE TABLE IF NOT EXISTS limit_operation(
    id              uuid PRIMARY KEY,
    operation_id    varchar(128),
    user_id         varchar(128),
    amount          numeric(19,2),
    type            varchar(32),
    status          varchar(32),
    created_at      timestamp,
    confirmed_at    timestamp,
    canceled_at     timestamp,
    reverted_at     timestamp,
    updated_at      timestamp)