-- Increment 2: the question becomes a structured form (SAD v2, section 2.3).
-- Questions from before have none of the new answers; they were test data and are removed.
DELETE FROM intake;

ALTER TABLE intake
    ADD COLUMN complaint_area       VARCHAR(32)  NOT NULL,
    ADD COLUMN complaint_area_other VARCHAR(100),
    ADD COLUMN duration             VARCHAR(32)  NOT NULL,
    ADD COLUMN severity             INTEGER      NOT NULL CHECK (severity BETWEEN 1 AND 10),
    ADD COLUMN symptoms             JSONB        NOT NULL,
    ADD COLUMN symptoms_other       VARCHAR(200),
    ADD COLUMN takes_medication     BOOLEAN      NOT NULL,
    ADD COLUMN current_medication   VARCHAR(500),
    ADD COLUMN tried_treatment      BOOLEAN      NOT NULL,
    ADD COLUMN treatment_tried      VARCHAR(500);
