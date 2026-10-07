CREATE TABLE intake (
    id           UUID          PRIMARY KEY,
    description  VARCHAR(1000) NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    submitted_at TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_intake_submitted_at ON intake (submitted_at);
