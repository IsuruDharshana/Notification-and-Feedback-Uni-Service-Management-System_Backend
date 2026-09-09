# Notification-and-Feedback-Uni-Service-Management-System_Backend

Spring Boot notification service. It owns the `notifications` table and exposes authenticated user APIs plus a service-to-service trigger endpoint.

## APIs

- `GET /api/notifications?unreadOnly=false&page=0&size=10` with `Authorization: Bearer <JWT>`; JWT subject must be the user UUID.
- `PATCH /api/notifications/{id}/read` with a user JWT.
- `POST /api/notifications/trigger` with `X-Service-Key`, for example from event-service when an event is cancelled.

The trigger contract is `{ "recipientId": "uuid", "message": "Your event has been cancelled.", "relatedType": "EVENT", "relatedId": "uuid" }`.

## Run

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `NOTIFICATION_SERVICE_KEY`, then run `mvn spring-boot:run`. Flyway creates the schema.
