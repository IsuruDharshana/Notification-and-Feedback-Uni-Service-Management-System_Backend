# Notification-and-Feedback-Uni-Service-Management-System_Backend

Spring Boot communication-feedback service for Group 8. It owns notification,
announcement/audience, and feedback data in its own schema. Other services use
documented APIs and identifiers; they must not read this database directly.

## APIs

- `GET /api/notifications?unreadOnly=false&page=0&size=10` with `Authorization: Bearer <JWT>`; JWT subject must be the user UUID.
- `PATCH /api/notifications/{id}/read` with a user JWT.
- `POST /api/notifications/trigger` with `X-Service-Key`, for example from event-service when an event is cancelled. The trigger contract uses `recipientId` (Group 5 string ID), `type`, `message`, `relatedType`, `relatedId`, `sourceService`, and `idempotencyKey`.
- `POST /api/announcements`, `POST /api/announcements/{id}/publish`, and `GET /api/announcements` with a user JWT.
- `POST /api/feedback/forms`, `GET /api/feedback/forms`, and `POST /api/feedback/forms/{formId}/responses` with a user JWT.
- `GET /api/engagement-dashboard/summary` with a user JWT.

The trigger contract is `{ "recipientId": "usr-student-001", "type": "EVENT_CANCELLED", "message": "Your event has been cancelled.", "relatedType": "EVENT", "relatedId": "uuid", "sourceService": "event-service", "idempotencyKey": "event-123-cancelled" }`.

The notification contract, request examples, response schemas, and error
responses are documented in `src/main/resources/openapi/notification-api.yaml`.

Announcement targeting supports `ALL`, `ROLE`, `DEPARTMENT`, `FACULTY`, and
`SERVICE_UNIT`. Published announcements now use the authenticated user's Group 5
profile for visibility checks. If Group 5 is not configured, the service returns
a safe `503` instead of exposing targeted data.

Feedback prevents duplicate submissions for the same user, form, and activity.
When `GROUP7_FEEDBACK_BASE_URL` is configured, submissions call Group 7's
feedback-eligibility endpoint. If it is blank, local synthetic mode permits
submissions for development only.

## Sprint 1 domain model

- `Announcement` and `AudienceRule` support draft/published/archived targeted communication.
- `Notification` supports event, registration, reservation, service-request, announcement, and external references.
- `FeedbackForm` belongs to one event or service request and stores its question definition as JSON text.
- `FeedbackResponse` stores a 1-5 rating and optional comment, with a database uniqueness rule for one response per user/activity/form.

External user, event, reservation, and service-request identifiers are stored as
UUID references without foreign keys because those records belong to other
microservices.

## Run

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `NOTIFICATIONS_SERVICE_KEY`, `USER_DIRECTORY_BASE_URL`, `FACILITY_DIRECTORY_BASE_URL` and `GROUP7_FEEDBACK_BASE_URL`, then run `mvn spring-boot:run`. Flyway creates the schema. The application intentionally has no development fallback for database credentials or JWT signing secrets.

The notification trigger validates `recipientId` through the Group 5 user directory; it does not read the Group 5 database directly. By default it calls `GET {USER_DIRECTORY_BASE_URL}/api/users/{userId}`. Set `USER_DIRECTORY_USER_PATH` if Group 5 agrees on a different path. A missing recipient returns `404 NOTIFICATION_RECIPIENT_NOT_FOUND`; an unconfigured or unavailable directory returns `503`.

Notification trigger idempotency is enforced by a unique `idempotencyKey` up to 200 characters. A new notification returns `201`; a replay returns `200` with the original notification. Invalid or missing `X-Service-Key` returns `401`. The accepted notification types are `REGISTRATION_CONFIRMED`, `REGISTRATION_CANCELLED`, `EVENT_CANCELLED`, and `EVENT_UPDATED`.

The Group 6 venue seam calls `GET {FACILITY_DIRECTORY_BASE_URL}/api/facilities/{venueId}` by default. The Group 7 feedback seam calls `GET {GROUP7_FEEDBACK_BASE_URL}/api/feedback-eligibility/{activityType}/{activityId}?userId={userId}` and expects `{ "eligible": true|false }`. Override the paths with `FACILITY_DIRECTORY_VENUE_PATH` and `GROUP7_FEEDBACK_ELIGIBILITY_PATH` when the provider contracts are finalized.

The engagement summary reports communication-feedback-owned metrics. Event participation is explicitly marked unavailable until the event/registration service supplies that data; it is not represented as a misleading zero.

## Verification

Run `mvn -s .mvn-settings.xml test` locally. GitHub Actions runs the same test
command for pushes to `main`/`master` and for pull requests.

## Run with Docker

Copy `.env.example` to `.env` and replace the example secrets. Then start the
application and its MySQL database with:

```text
docker compose up --build -d
```

The service is available at `http://localhost:8082` and its health endpoint is
`http://localhost:8082/actuator/health`. Flyway runs the database migrations on
startup. Stop the containers with `docker compose down`; the MySQL data remains
in the `mysql_data` Docker volume.

For a hosted deployment, build the image with `docker build -t communication-feedback-service .`.
The image runs as a non-root user, listens on the `PORT` environment variable
(default `8082`), and requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, and `NOTIFICATIONS_SERVICE_KEY` at runtime.
