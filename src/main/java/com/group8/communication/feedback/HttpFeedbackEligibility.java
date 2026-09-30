package com.group8.communication.feedback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Who may rate an activity (BR8-07):
 * EVENT - a CONFIRMED registration for the event (event-service {@code GET /api/registrations/mine});
 * SERVICE_REQUEST - the requester of a RESOLVED or CLOSED request (Group 7
 * {@code GET /api/work-orders/by-request/{requestId}}).
 * The caller's bearer token is forwarded. A provider whose base URL is not configured allows everyone
 * (local development without that service).
 */
@Component
public class HttpFeedbackEligibility implements FeedbackEligibility {
    static final Set<String> RATEABLE_REQUEST_STATUSES = Set.of("RESOLVED", "CLOSED");

    private final RestClient restClient;
    private final String group7BaseUrl;
    private final String group7Path;
    private final String eventServiceBaseUrl;
    private final String registrationsPath;

    public HttpFeedbackEligibility(
            RestClient.Builder restClientBuilder,
            @Value("${group7-feedback.base-url:}") String group7BaseUrl,
            @Value("${group7-feedback.eligibility-path:/api/work-orders/by-request/{requestId}}") String group7Path,
            @Value("${event-service.base-url:}") String eventServiceBaseUrl,
            @Value("${event-service.registrations-path:/api/registrations/mine}") String registrationsPath) {
        this.restClient = restClientBuilder.build();
        this.group7BaseUrl = trimSlash(group7BaseUrl);
        this.group7Path = withSlash(group7Path, "/api/work-orders/by-request/{requestId}");
        this.eventServiceBaseUrl = trimSlash(eventServiceBaseUrl);
        this.registrationsPath = withSlash(registrationsPath, "/api/registrations/mine");
    }

    @Override
    public boolean canSubmit(String respondentId, FeedbackForm form) {
        return switch (form.getActivityType()) {
            case EVENT -> attendedEvent(respondentId, form.getActivityId());
            case SERVICE_REQUEST -> requestedAndResolved(respondentId, form.getActivityId());
        };
    }

    private boolean attendedEvent(String respondentId, String eventId) {
        if (eventServiceBaseUrl.isBlank()) return true;
        try {
            JsonNode body = authorized(restClient.get().uri(eventServiceBaseUrl + registrationsPath))
                    .retrieve().body(JsonNode.class);
            JsonNode registrations = body != null && body.has("data") ? body.get("data") : body;
            if (registrations == null || !registrations.isArray()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "EVENT_SERVICE_INVALID_RESPONSE");
            }
            for (JsonNode registration : registrations) {
                if (eventId.equalsIgnoreCase(text(registration, "eventId"))
                        && "CONFIRMED".equals(text(registration, "status"))
                        && respondentId.equals(textOr(registration, "userId", respondentId))) {
                    return true;
                }
            }
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "EVENT_SERVICE_UNAVAILABLE", exception);
        }
    }

    private boolean requestedAndResolved(String respondentId, String requestId) {
        if (group7BaseUrl.isBlank()) return true;
        try {
            JsonNode body = authorized(restClient.get().uri(group7BaseUrl + group7Path, requestId))
                    .retrieve().body(JsonNode.class);
            JsonNode workOrder = body != null && body.has("data") ? body.get("data") : body;
            if (workOrder == null || !workOrder.isObject()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GROUP7_INVALID_ELIGIBILITY_RESPONSE");
            }
            String status = text(workOrder, "status");
            return status != null
                    && RATEABLE_REQUEST_STATUSES.contains(status.toUpperCase())
                    && respondentId.equals(text(workOrder, "requesterId"));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FEEDBACK_ACTIVITY_NOT_FOUND", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GROUP7_ELIGIBILITY_UNAVAILABLE", exception);
        }
    }

    private static RestClient.RequestHeadersSpec<?> authorized(RestClient.RequestHeadersSpec<?> request) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String header = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) return request.header(HttpHeaders.AUTHORIZATION, header);
        }
        return request;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() ? value.stringValue() : null;
    }

    private static String textOr(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return value == null ? fallback : value;
    }

    private static String trimSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static String withSlash(String path, String fallback) {
        if (path == null || path.isBlank()) return fallback;
        return path.startsWith("/") ? path : "/" + path;
    }
}
