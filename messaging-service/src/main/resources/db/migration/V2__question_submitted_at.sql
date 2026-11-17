-- When the question of a reply was sent, so the reply is deleted together with its question after 6 months.
ALTER TABLE reply ADD COLUMN question_submitted_at TIMESTAMPTZ;
-- Replies from before this column: their own time is the closest known value (a reply comes after its question).
UPDATE reply SET question_submitted_at = created_at WHERE question_submitted_at IS NULL;
ALTER TABLE reply ALTER COLUMN question_submitted_at SET NOT NULL;

CREATE INDEX idx_reply_question_submitted_at ON reply (question_submitted_at);
