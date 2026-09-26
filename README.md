# Notification-and-Feedback-Uni-Service-Management-System_Backend

Spring Boot communication-feedback service for Group 8. It owns notification,
announcement/audience, and feedback data in its own schema. Other services use
documented APIs and identifiers; they must not read this database directly.

## APIs

- `GET /api/notifications?unreadOnly=false&page=0&size=10` with `Authorization: Bearer <JWT>`; JWT subject must be the user UUID.
- `PATCH /api/notifications/{id}/read` with a user JWT.
- `POST /api/notifications/trigger` with `X-Service-Key`, for example from event-service when an event is cancelled.

The trigger contract is `{ "recipientId": "uuid", "message": "Your event has been cancelled.", "relatedType": "EVENT", "relatedId": "uuid" }`.

The notification contract, request examples, response schemas, and error
responses are documented in `src/main/resources/openapi/notification-api.yaml`.

## Sprint 1 domain model

- `Announcement` and `AudienceRule` support draft/published/archived targeted communication.
- `Notification` supports event, registration, reservation, service-request, announcement, and external references.
- `FeedbackForm` belongs to one event or service request and stores its question definition as JSON text.
- `FeedbackResponse` stores a 1-5 rating and optional comment, with a database uniqueness rule for one response per user/activity/form.

External user, event, reservation, and service-request identifiers are stored as
UUID references without foreign keys because those records belong to other
microservices.

## Run

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `NOTIFICATION_SERVICE_KEY`, then run `mvn spring-boot:run`. Flyway creates the schema.
