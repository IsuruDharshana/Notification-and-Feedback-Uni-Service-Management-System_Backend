ALTER TABLE announcements
    MODIFY COLUMN created_by VARCHAR(64) NOT NULL;

ALTER TABLE feedback_forms
    MODIFY COLUMN created_by VARCHAR(64) NOT NULL;

ALTER TABLE feedback_responses
    MODIFY COLUMN respondent_id VARCHAR(64) NOT NULL;

ALTER TABLE notifications
    MODIFY COLUMN related_id VARCHAR(128) NULL;

ALTER TABLE notifications
    DROP CONSTRAINT chk_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type CHECK (notification_type IN (
        'REGISTRATION_CONFIRMED',
        'REGISTRATION_CANCELLED',
        'EVENT_CANCELLED',
        'EVENT_UPDATED',
        'RESERVATION_STATUS',
        'SERVICE_REQUEST_STATUS',
        'LEGACY'
    ));
