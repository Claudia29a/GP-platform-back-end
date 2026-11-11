-- Who opened which question and when (GDPR Article 32; NEN 7513 in Dutch healthcare).
-- The staff member's name is encrypted and their ID hashed. The log is kept after a question is deleted:
-- it is the record of who accessed it, and it holds no patient data.
CREATE TABLE access_log (
    id         UUID        PRIMARY KEY,
    intake_id  UUID        NOT NULL,
    staff_hash VARCHAR(64) NOT NULL,
    staff_name TEXT        NOT NULL,
    staff_role VARCHAR(20) NOT NULL,
    viewed_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_access_log_intake ON access_log (intake_id, viewed_at);
