CREATE TABLE IF NOT EXISTS user_limit_account(
    user_id             varchar(128),
    available_amount    numeric(19,2),
    reserved_amount     numeric(19,2),
    last_reset_date     date,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    version             bigint);