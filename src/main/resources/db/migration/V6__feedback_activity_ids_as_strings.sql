-- Group 7 request ids are not UUIDs (e.g. REQ-2026-004), so feedback activity ids are stored as strings.
ALTER TABLE feedback_forms
    MODIFY COLUMN activity_id VARCHAR(64) NOT NULL;

ALTER TABLE feedback_responses
    MODIFY COLUMN activity_id VARCHAR(64) NOT NULL;
