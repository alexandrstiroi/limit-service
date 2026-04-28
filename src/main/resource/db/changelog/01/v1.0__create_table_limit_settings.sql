CREATE TABLE IF NOT EXISTS limit_settings (
    id bigserial,
    default_limit numeric(19,2),
    updated_at timestamp
);