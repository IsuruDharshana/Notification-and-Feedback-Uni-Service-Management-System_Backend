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

The trigger contract is `{ "recipientId": "usr-student-001", "type": "EVENT_CANCELLED", "message": "Your event has been cancelled.", "relatedType": "EVENT", "relatedId": "event-123", "sourceService": "event-service", "idempotencyKey": "event-123-cancelled" }`. Supported trigger types also include `RESERVATION_STATUS` and `SERVICE_REQUEST_STATUS`.

The notification contract, request examples, response schemas, and error
responses are documented in `src/main/resources/openapi/notification-api.yaml`.

Announcement targeting supports `ALL`, `ROLE`, `DEPARTMENT`, `FACULTY`, and
`SERVICE_UNIT`. Published announcements now use the authenticated user's Group 5
roles and department/faculty affiliation (from Group 5's eligibility endpoint) for
visibility checks. If Group 5 is not configured or unavailable, only `ALL` and
`ROLE` announcements are matched, using the roles in the caller's token.

Feedback prevents duplicate submissions for the same user, form, and activity.
Service-request feedback calls Group 7's `/api/work-orders/by-request/{requestId}`
(`GROUP7_FEEDBACK_BASE_URL`) and is accepted only from the requester of a `RESOLVED`
or `CLOSED` request. Event feedback calls event-service `/api/registrations/mine`
(`EVENT_SERVICE_BASE_URL`) and needs a `CONFIRMED` registration for the event. Both
forward the caller's token. A blank base URL permits submissions (development only).
Forms can be created by `EVENT_ORGANIZER`, `ACADEMIC_STAFF`, `ADMINISTRATIVE_STAFF` and `ADMIN`.

## Sprint 1 domain model

- `Announcement` and `AudienceRule` support draft/published/archived targeted communication.
- `Notification` supports event, registration, reservation, service-request, announcement, and external references.
- `FeedbackForm` belongs to one event or service request and stores its question definition as JSON text.
- `FeedbackResponse` stores a 1-5 rating and optional comment, with a database uniqueness rule for one response per user/activity/form.

External user, event, reservation, and service-request identifiers are stored as
string references (Group 5 ids such as `usr-student-001`, Group 7 ids such as `REQ-2026-004`) without foreign keys because those records belong to other
microservices.

## Run

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GROUP5_JWKS_URL`, `GROUP5_JWT_ISSUER`, `GROUP5_JWT_AUDIENCE`, `NOTIFICATIONS_SERVICE_KEY`, `USER_DIRECTORY_BASE_URL`, `FACILITY_DIRECTORY_BASE_URL`, `GROUP7_FEEDBACK_BASE_URL` and `EVENT_SERVICE_BASE_URL`, then run `mvn spring-boot:run`. Flyway creates the schema. Group 5 user JWTs are verified with RS256 through its JWKS endpoint.

The notification trigger validates `recipientId` through the Group 5 identity service; it does not read the Group 5 database directly. By default it calls `GET {USER_DIRECTORY_BASE_URL}/api/v1/validation/users/{userId}/eligibility` and forwards the incoming Bearer token when present. `USER_DIRECTORY_ACCESS_TOKEN` is an optional fallback and should contain only a Group 5 approved machine-to-machine credential, never a personal login token. A missing or ineligible recipient returns `404 NOTIFICATION_RECIPIENT_NOT_FOUND`; an unavailable directory returns `503`. Trigger calls carry no user token, so without `USER_DIRECTORY_ACCESS_TOKEN` (or when Group 5 rejects it) the recipient is accepted unverified and a warning is logged - the caller is already trusted through `X-Service-Key`.

Notification trigger idempotency is enforced by a unique `idempotencyKey` up to 200 characters. A new notification returns `201`; a replay returns `200` with the original notification. Invalid or missing `X-Service-Key` returns `401`. The accepted notification types are `REGISTRATION_CONFIRMED`, `REGISTRATION_CANCELLED`, `EVENT_CANCELLED`, `EVENT_UPDATED`, `RESERVATION_STATUS`, and `SERVICE_REQUEST_STATUS`.

The Group 6 venue seam calls `GET {FACILITY_DIRECTORY_BASE_URL}/api/facilities/{venueId}` by default. Override the paths with `FACILITY_DIRECTORY_VENUE_PATH` and `GROUP7_FEEDBACK_ELIGIBILITY_PATH` when the provider contracts are finalized.

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
the Group 5 JWKS settings, and `NOTIFICATIONS_SERVICE_KEY` at runtime.
