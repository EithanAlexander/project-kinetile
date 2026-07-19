CREATE TABLE api_rate_limit_clients (
    client_ip VARCHAR(45) NOT NULL PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    violation_count INTEGER NOT NULL DEFAULT 0,
    first_violation_at TIMESTAMPTZ,
    penalty_until TIMESTAMPTZ,
    permanently_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_api_rate_limit_clients_last_seen_at ON api_rate_limit_clients (last_seen_at);
