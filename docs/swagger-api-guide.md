# Testing the Group 8 API with Swagger

## Open the documentation

1. Run the service locally (`docker compose up --build -d` with your `.env` configured),
   or deploy the version containing Swagger to Render.
2. Open `<service-base-url>/swagger-ui.html`. Local default: `http://localhost:8082/swagger-ui.html`.
3. Expand an operation to see its inputs, examples, expected responses and permissions.
4. Use **Authorize**, then **Try it out** and **Execute** to send a request.

The JSON specification is at `/v3/api-docs`; YAML is at `/v3/api-docs.yaml`.
Postman can import either URL. All three documentation URLs are readable without
a login. Business API authentication remains enabled.

## Choose the correct authorization

| Operations | Swagger authorization | Additional rules |
| --- | --- | --- |
| `POST /api/notifications/trigger` | `serviceKey` | The value must match the server's `NOTIFICATIONS_SERVICE_KEY`. |
| List notifications / mark read | `bearerAuth` | Only the signed-in user's inbox; marking read requires ownership. |
| List announcements | `bearerAuth` | Only published announcements matching the user's audience. |
| Create / publish / archive announcements | `bearerAuth` | `ADMIN` or `ADMINISTRATIVE_STAFF`; publish/archive also require creator ownership. |
| Create feedback forms | `bearerAuth` | `EVENT_ORGANIZER`, `ACADEMIC_STAFF`, `ADMINISTRATIVE_STAFF` or `ADMIN`. |
| List / get feedback forms | `bearerAuth` | Any authenticated user. |
| Submit feedback | `bearerAuth` | Active form, eligible participant/requester, one response per user/form/activity. |
| List feedback responses | `bearerAuth` | Only the form creator. |
| Engagement summary | `bearerAuth` | Any authenticated user. |

For `bearerAuth`, paste only a valid Group 5 access token, **not** the word
`Bearer`. Its subject is a Group 5 internal string id, not a UUID. Your roles come
from that token; Swagger cannot grant roles. Tokens expire, so a previously
working token may later return `401`.

For `serviceKey`, enter the private shared key, **not** a user JWT. Do not put a
personal login token into Render environment variables. Authorization is not
persisted across Swagger page reloads. Do not share screenshots containing secrets.

## Notification trigger example

Authorize `serviceKey`, select `POST /api/notifications/trigger`, then use:

```json
{
  "recipientId": "usr-student-001",
  "type": "EVENT_CANCELLED",
  "message": "Your test event has been cancelled.",
  "relatedType": "EVENT",
  "relatedId": "event-swagger-test-001",
  "sourceService": "event-service",
  "idempotencyKey": "swagger-event-cancelled-usr-student-001-001"
}
```

- First use of the key: `201`, with the stored notification.
- Repeat the same request: `200`, returning the original notification.
- Missing/wrong service key with a valid body: `401 INVALID_SERVICE_KEY`.
- Use a new idempotency key for a different intended notification. A reused key
  returns the original record even if you change the payload.
- `recipientId`: at most 64 characters; `idempotencyKey`: at most 200;
  `relatedId`: at most 128. `relatedId` may be omitted/null only with `EXTERNAL`.
- Accepted types: `REGISTRATION_CONFIRMED`, `REGISTRATION_CANCELLED`,
  `EVENT_CANCELLED`, `EVENT_UPDATED`, `RESERVATION_STATUS`, `SERVICE_REQUEST_STATUS`.
  `LEGACY` is not accepted for new triggers.

This stores an **in-app** notification; it does not send an email/SMS. To read it
through `GET /api/notifications`, authenticate as the recipient. A different
student's token will correctly show a different inbox.

Recipient-directory behavior is documented as implemented: verification needs a
configured Group 5 URL and credential. Missing settings or a Group 5 `401`/`403`
allow a trusted trigger without recipient verification and log a warning. Other
provider failures can return `503`. Swagger does not change or bypass this logic.

## Feedback example

With an allowed organizer/staff/admin token, create a form using
`POST /api/feedback/forms`:

```json
{
  "activityType": "SERVICE_REQUEST",
  "activityId": "REQ-2026-004",
  "title": "Service request feedback",
  "questionsJson": "[{\"id\":\"overall\",\"label\":\"How was the service?\",\"type\":\"rating\"}]"
}
```

Replace the activity id with a real test record. `questionsJson` is a JSON-encoded
**string**, not an array property. Save the returned form `id` (a UUID).

As an eligible requester, submit to
`POST /api/feedback/forms/{formId}/responses`:

```json
{
  "rating": 5,
  "comment": "The issue was resolved successfully."
}
```

Group 7 service-request eligibility requires the requester and a `RESOLVED` or
`CLOSED` request. Event eligibility requires a `CONFIRMED` registration in
event-service. These checks need the provider URLs and a valid caller token.
Blank provider URLs currently permit submissions for development; do not treat
that as evidence that the live integration has passed.

## Deployment / troubleshooting checklist

1. Run `mvn -B -s .mvn-settings.xml verify` before deploying.
2. Deploy the commit containing the Swagger changes, not an older branch/image.
3. Check `/actuator/health`, then `/v3/api-docs`, then `/swagger-ui.html`.
4. A `401` on a business API means missing/invalid authentication; a `403` usually
   means role, ownership or eligibility requirements were not met.
5. A `500` from `/v3/api-docs` needs the matching application log stack trace.
   A `503` from a business API may be a dependency issue, not a Swagger issue.

The integration test checks documentation generation with the actual Boot 4,
Jackson 3, security and HTTP stack. Its repositories are mocked, so it does not
claim to verify the hosted database, real tokens or other teams' live services.
