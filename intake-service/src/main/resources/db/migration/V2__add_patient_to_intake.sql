-- From now on every question belongs to the patient who asked it.
-- Questions submitted before sign-in existed have no owner; they were test data and are removed.
DELETE FROM intake;

ALTER TABLE intake
    ADD COLUMN patient_id    VARCHAR(255) NOT NULL,
    ADD COLUMN patient_name  VARCHAR(255) NOT NULL,
    ADD COLUMN patient_email VARCHAR(320) NOT NULL;

CREATE INDEX idx_intake_patient_id ON intake (patient_id);
