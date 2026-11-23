-- User story nr 9: whether the GP asked the patient to make an appointment, stored encrypted ("yes"/"no").
-- Replies from before this column stay empty, which the service reads as "no".
ALTER TABLE reply ADD COLUMN appointment TEXT;
