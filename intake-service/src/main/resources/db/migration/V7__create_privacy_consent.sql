-- Who allowed the use of their data, and which version of the privacy text they saw (GDPR Article 7).
-- The user is stored as a hash, like the owner of a question.
CREATE TABLE privacy_consent (
    user_hash   VARCHAR(64) PRIMARY KEY,
    version     VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL
);
