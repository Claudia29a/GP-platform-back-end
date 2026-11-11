-- Email verification: a 6-digit code is emailed after sign-up. Only a hash of the code is stored.
ALTER TABLE account
    ADD COLUMN verification_code_hash    VARCHAR(64),
    ADD COLUMN verification_expires_at   TIMESTAMPTZ,
    ADD COLUMN verification_attempts     INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_code_sent_at         TIMESTAMPTZ,
    ADD COLUMN codes_sent_since          TIMESTAMPTZ,
    ADD COLUMN codes_sent_count          INTEGER NOT NULL DEFAULT 0;

-- Recent sign-ups per visitor, to stop accounts being created in bulk. The IP address is stored as a hash
-- and every row is removed after an hour.
CREATE TABLE sign_up (
    id         UUID        PRIMARY KEY,
    ip_hash    VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sign_up_ip ON sign_up (ip_hash, created_at);
