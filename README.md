# Notification-and-Feedback-Uni-Service-Management-System_Backend

Spring Boot communication-feedback service for Group 8. It owns notification,
announcement/audience, and feedback data in its own schema. Other services use
documented APIs and identifiers; they must not read this database directly.

## APIs

- `GET /api/notifications?unreadOnly=false&page=0&size=10` with `Authorization: Bearer <JWT>`; JWT subject must be the user UUID.
- `PATCH /api/notifications/{id}/read` with a user JWT.
- `POST /api/notifications/trigger` with `X-Service-Key`, for example from event-service when an event is cancelled.
- `POST /api/announcements`, `POST /api/announcements/{id}/publish`, and `GET /api/announcements` with a user JWT.
- `POST /api/feedback/forms`, `GET /api/feedback/forms`, and `POST /api/feedback/forms/{formId}/responses` with a user JWT.

The trigger contract is `{ "recipientId": "uuid", "message": "Your event has been cancelled.", "relatedType": "EVENT", "relatedId": "uuid" }`.

The notification contract, request examples, response schemas, and error
responses are documented in `src/main/resources/openapi/notification-api.yaml`.

Announcement targeting supports `ALL`, `ROLE`, `DEPARTMENT`, `FACULTY`, and
`SERVICE_UNIT`. Sprint 2 uses the supplied audience context for basic matching;
real Group 5 role/department/faculty validation remains a cross-service Sprint 3 integration.

Feedback prevents duplicate submissions for the same user, form, and activity.
The `FeedbackEligibility` interface is the Group 7 integration seam; Sprint 2
uses a permissive synthetic implementation until Group 7 exposes its completion-status contract.

## Sprint 1 domain model

- `Announcement` and `AudienceRule` support draft/published/archived targeted communication.
- `Notification` supports event, registration, reservation, service-request, announcement, and external references.
- `FeedbackForm` belongs to one event or service request and stores its question definition as JSON text.
- `FeedbackResponse` stores a 1-5 rating and optional comment, with a database uniqueness rule for one response per user/activity/form.

External user, event, reservation, and service-request identifiers are stored as
UUID references without foreign keys because those records belong to other
microservices.

## Run

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `NOTIFICATION_SERVICE_KEY` and `USER_DIRECTORY_BASE_URL`, then run `mvn spring-boot:run`. Flyway creates the schema.

The notification trigger validates `recipientId` through the Group 5 user directory; it does not read the Group 5 database directly. By default it calls `GET {USER_DIRECTORY_BASE_URL}/api/users/{userId}`. Set `USER_DIRECTORY_USER_PATH` if Group 5 agrees on a different path. A missing recipient returns `404 NOTIFICATION_RECIPIENT_NOT_FOUND`; an unconfigured or unavailable directory returns `503`.

## Verification

Run `mvn -s .mvn-settings.xml test` locally. GitHub Actions runs the same test
command for pushes to `main`/`master` and for pull requests.
