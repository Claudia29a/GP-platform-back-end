-- Accounts for people who sign in with email and password instead of Google (ADR-010).
CREATE TABLE account (
    id                    UUID         PRIMARY KEY,
    email                 VARCHAR(320) NOT NULL,
    name                  VARCHAR(100) NOT NULL,
    -- BCrypt hash; the password itself is never stored.
    password_hash         VARCHAR(100) NOT NULL,
    -- False until the person proves the address is theirs (email verification comes later).
    email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL
);

-- One account per email address, compared without capitals.
CREATE UNIQUE INDEX uq_account_email ON account (LOWER(email));
