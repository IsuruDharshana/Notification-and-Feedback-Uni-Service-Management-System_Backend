package com.group8.communication.feedback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpFeedbackEligibilityTest {
    private static final String EVENT_ID = "a0000000-0000-0000-0000-000000000001";

    private MockRestServiceServer server;
    private HttpFeedbackEligibility eligibility;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        eligibility = new HttpFeedbackEligibility(builder, "http://g7", "/api/work-orders/by-request/{requestId}",
                "http://events", "/api/registrations/mine");
    }

    private static FeedbackForm form(FeedbackActivityType type, String activityId) {
        return new FeedbackForm(type, activityId, "Feedback", "[]", "usr-organizer-001");
    }

    private void workOrder(String status, String requester) {
        server.expect(requestTo("http://g7/api/work-orders/by-request/REQ-2026-004"))
                .andRespond(withSuccess("{\"requestId\":\"REQ-2026-004\",\"status\":\"" + status
                        + "\",\"requesterId\":\"" + requester + "\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void requesterOfResolvedRequestMayRate() {
        workOrder("RESOLVED", "usr-student-001");
        assertTrue(eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
    }

    @Test
    void requesterOfClosedRequestMayRate() {
        workOrder("CLOSED", "usr-student-001");
        assertTrue(eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
    }

    @Test
    void openRequestCannotBeRated() {
        workOrder("IN_PROGRESS", "usr-student-001");
        assertFalse(eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
    }

    @Test
    void onlyTheRequesterMayRate() {
        workOrder("RESOLVED", "usr-student-001");
        assertFalse(eligibility.canSubmit("usr-student-002", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
    }

    @Test
    void unknownRequestIsNotFound() {
        server.expect(requestTo("http://g7/api/work-orders/by-request/REQ-2026-004")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void group7OutageIs503() {
        server.expect(requestTo("http://g7/api/work-orders/by-request/REQ-2026-004")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
        assertEquals(503, exception.getStatusCode().value());
    }

    @Test
    void confirmedRegistrationMayRateEvent() {
        server.expect(requestTo("http://events/api/registrations/mine")).andRespond(withSuccess(
                "[{\"eventId\":\"" + EVENT_ID + "\",\"userId\":\"usr-student-001\",\"status\":\"CONFIRMED\"}]",
                MediaType.APPLICATION_JSON));
        assertTrue(eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.EVENT, EVENT_ID)));
    }

    @Test
    void cancelledOrMissingRegistrationCannotRateEvent() {
        server.expect(requestTo("http://events/api/registrations/mine")).andRespond(withSuccess(
                "[{\"eventId\":\"" + EVENT_ID + "\",\"userId\":\"usr-student-001\",\"status\":\"CANCELLED\"}]",
                MediaType.APPLICATION_JSON));
        assertFalse(eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.EVENT, EVENT_ID)));
    }

    @Test
    void eventFeedbackDoesNotCallGroup7() {
        server.expect(requestTo("http://events/api/registrations/mine")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertFalse(eligibility.canSubmit("usr-student-001", form(FeedbackActivityType.EVENT, EVENT_ID)));
        server.verify();
    }

    @Test
    void unconfiguredProvidersAllowSubmission() {
        HttpFeedbackEligibility local = new HttpFeedbackEligibility(RestClient.builder(), "", "", "", "");
        assertTrue(local.canSubmit("usr-student-001", form(FeedbackActivityType.EVENT, EVENT_ID)));
        assertTrue(local.canSubmit("usr-student-001", form(FeedbackActivityType.SERVICE_REQUEST, "REQ-2026-004")));
    }
}
