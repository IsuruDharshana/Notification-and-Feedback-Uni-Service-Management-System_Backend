CREATE TABLE notifications (
    id CHAR(36) NOT NULL,
    recipient_id CHAR(36) NOT NULL,
    message TEXT NOT NULL,
    related_type VARCHAR(20) NOT NULL,
    related_id CHAR(36) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_notifications_related_type CHECK (related_type IN ('EVENT','REGISTRATION','ANNOUNCEMENT','EXTERNAL')),
    INDEX idx_notifications_recipient_read_created (recipient_id, is_read, created_at)
);
