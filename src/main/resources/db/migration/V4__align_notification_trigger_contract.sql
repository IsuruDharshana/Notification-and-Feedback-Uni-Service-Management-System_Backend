ALTER TABLE notifications
    MODIFY COLUMN recipient_id VARCHAR(64) NOT NULL;

ALTER TABLE notifications
    ADD COLUMN notification_type VARCHAR(64) NULL,
    ADD COLUMN source_service VARCHAR(64) NULL,
    ADD COLUMN idempotency_key VARCHAR(200) NULL;

UPDATE notifications
SET notification_type = 'LEGACY',
    source_service = 'legacy',
    idempotency_key = CONCAT('legacy-', id)
WHERE notification_type IS NULL;

ALTER TABLE notifications
    MODIFY COLUMN notification_type VARCHAR(64) NOT NULL,
    MODIFY COLUMN source_service VARCHAR(64) NOT NULL,
    MODIFY COLUMN idempotency_key VARCHAR(200) NOT NULL;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type CHECK (notification_type IN (
        'REGISTRATION_CONFIRMED', 'REGISTRATION_CANCELLED', 'EVENT_CANCELLED', 'EVENT_UPDATED', 'LEGACY'
    )),
    ADD CONSTRAINT uk_notifications_idempotency_key UNIQUE (idempotency_key);
