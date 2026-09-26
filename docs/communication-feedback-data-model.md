# Communication and Feedback Data Model

Sprint 1 foundation for the Group 8 `communication-feedback-service`.

## Ownership boundary

This service owns announcements, audience rules, notifications, feedback forms,
and feedback responses. User, event, reservation, and service-request records
remain owned by their provider services. Cross-service records are referenced by
UUID and are not represented with database foreign keys.

## Relationships

```mermaid
erDiagram
    AUDIENCE_RULE ||--|| ANNOUNCEMENT : targets
    FEEDBACK_FORM ||--o{ FEEDBACK_RESPONSE : receives

    AUDIENCE_RULE {
        uuid id PK
        enum audience_type
        string rule_value
        datetime created_at
    }

    ANNOUNCEMENT {
        uuid id PK
        string title
        text content
        enum status
        uuid created_by FK_external_user
        uuid audience_rule_id FK
        datetime published_at
        datetime created_at
        datetime updated_at
    }

    NOTIFICATION {
        uuid id PK
        uuid recipient_id FK_external_user
        text message
        enum related_type
        uuid related_id FK_external_record
        boolean is_read
        datetime created_at
    }

    FEEDBACK_FORM {
        uuid id PK
        enum activity_type
        uuid activity_id FK_external_event_or_request
        string title
        json questions_json
        boolean active
        datetime created_at
        datetime updated_at
    }

    FEEDBACK_RESPONSE {
        uuid id PK
        uuid form_id FK
        uuid activity_id FK_external_event_or_request
        uuid respondent_id FK_external_user
        int rating
        text comment
        datetime created_at
    }
```

## Domain rules captured by the foundation

- An announcement has one audience rule and starts in `DRAFT` status.
- Audience types are `ALL`, `ROLE`, `DEPARTMENT`, `FACULTY`, and `SERVICE_UNIT`.
- Notifications can reference events, registrations, announcements, reservations,
  service requests, or an external workflow.
- A feedback form belongs to one event or service request.
- A feedback form has one response per user/activity/form combination.
- Feedback ratings are restricted to 1 through 5.
- `questions_json` is stored as service-owned text for now so the question shape
  can evolve without a cross-service database dependency.

## Migration

`V2__create_communication_feedback_domain_tables.sql` creates the four new
domain tables and expands the notification reference-type constraint. The
existing `V1__create_notifications_table.sql` remains unchanged because Flyway
migrations are append-only after they have been applied.
