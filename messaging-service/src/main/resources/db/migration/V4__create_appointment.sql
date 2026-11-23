-- User story nr 10: appointments patients book after the GP's invitation (a reply).
-- One appointment per start time and per invitation, guaranteed by the database even when two patients book at the
-- same moment. An appointment goes when its reply goes: with the account, and with the 6-month clean-up.
CREATE TABLE appointment (
    id           UUID        PRIMARY KEY,
    reply_id     UUID        NOT NULL REFERENCES reply (id) ON DELETE CASCADE,
    intake_id    UUID        NOT NULL,
    patient_hash VARCHAR(64) NOT NULL,
    patient_name TEXT        NOT NULL,
    starts_at    TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_appointment_reply UNIQUE (reply_id),
    CONSTRAINT uq_appointment_starts_at UNIQUE (starts_at)
);
