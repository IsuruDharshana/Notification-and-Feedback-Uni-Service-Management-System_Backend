ALTER TABLE notifications DROP CONSTRAINT chk_notifications_related_type;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_related_type
        CHECK (related_type IN ('EVENT', 'REGISTRATION', 'ANNOUNCEMENT', 'RESERVATION', 'SERVICE_REQUEST', 'EXTERNAL'));

CREATE TABLE audience_rules (
    id CHAR(36) NOT NULL,
    audience_type VARCHAR(20) NOT NULL,
    rule_value VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_audience_rules_type CHECK (audience_type IN ('ALL', 'ROLE', 'DEPARTMENT', 'FACULTY', 'SERVICE_UNIT'))
);

CREATE TABLE announcements (
    id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by CHAR(36) NOT NULL,
    audience_rule_id CHAR(36) NOT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_announcements_audience_rule (audience_rule_id),
    CONSTRAINT fk_announcements_audience_rule FOREIGN KEY (audience_rule_id) REFERENCES audience_rules (id),
    CONSTRAINT chk_announcements_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE feedback_forms (
    id CHAR(36) NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    activity_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    questions_json TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_forms_activity (activity_type, activity_id),
    CONSTRAINT chk_feedback_forms_activity_type CHECK (activity_type IN ('EVENT', 'SERVICE_REQUEST'))
);

CREATE TABLE feedback_responses (
    id CHAR(36) NOT NULL,
    form_id CHAR(36) NOT NULL,
    activity_id CHAR(36) NOT NULL,
    respondent_id CHAR(36) NOT NULL,
    rating INT NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_response_user_activity (form_id, respondent_id, activity_id),
    CONSTRAINT fk_feedback_responses_form FOREIGN KEY (form_id) REFERENCES feedback_forms (id),
    CONSTRAINT chk_feedback_responses_rating CHECK (rating BETWEEN 1 AND 5)
);
