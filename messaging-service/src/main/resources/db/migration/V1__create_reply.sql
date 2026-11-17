-- The GP's replies to questions. The question itself stays in the Intake Service; only its ID is kept here.
-- The reply and the GP's name are encrypted, the GP's account ID is hashed.
CREATE TABLE reply (
    id          UUID        PRIMARY KEY,
    intake_id   UUID        NOT NULL,
    author_hash VARCHAR(64) NOT NULL,
    author_name TEXT        NOT NULL,
    text        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reply_intake ON reply (intake_id, created_at);
