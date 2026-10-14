-- Increment 3: the severity-ordered queue (SAD v2, section 2.3, process 2).
ALTER TABLE intake
    ADD COLUMN severity_score INTEGER,
    ADD COLUMN queue_position INTEGER;

-- Score the questions that already exist, with the same rule as SeverityCalculator.
UPDATE intake SET severity_score = LEAST(100,
      severity * 6
    + CASE duration
          WHEN 'LESS_THAN_A_DAY'    THEN 0
          WHEN 'ONE_TO_THREE_DAYS'  THEN 5
          WHEN 'FOUR_TO_SEVEN_DAYS' THEN 10
          WHEN 'ONE_TO_FOUR_WEEKS'  THEN 15
          WHEN 'MORE_THAN_A_MONTH'  THEN 20
      END
    + LEAST(20, 4 * jsonb_array_length(symptoms)));

-- Put them in the queue: highest score first, equal scores oldest first.
UPDATE intake
SET queue_position = ranked.position
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY severity_score DESC, submitted_at ASC) AS position FROM intake) AS ranked
WHERE intake.id = ranked.id;

ALTER TABLE intake
    ALTER COLUMN severity_score SET NOT NULL,
    ADD CONSTRAINT intake_severity_score_check CHECK (severity_score BETWEEN 0 AND 100);

CREATE INDEX idx_intake_queue ON intake (status, queue_position);
