-- Story 1 TAC3: an open question has the status "In review".
UPDATE intake SET status = 'IN_REVIEW' WHERE status = 'SUBMITTED';

-- Story 4 TAC4: marks questions staff moved up by hand. A new question never goes above one of these.
ALTER TABLE intake ADD COLUMN moved_by_hand BOOLEAN NOT NULL DEFAULT FALSE;

-- A question already above a more severe one can only have got there by hand.
UPDATE intake AS question
SET moved_by_hand = EXISTS (
    SELECT 1 FROM intake AS below
    WHERE below.status = question.status
      AND below.queue_position > question.queue_position
      AND below.severity_score > question.severity_score)
WHERE question.status = 'IN_REVIEW';
