package com.group8.communication.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class HttpFeedbackEligibility implements FeedbackEligibility {
    private final RestClient restClient;
    private final String baseUrl;
    private final String eligibilityPath;

    public HttpFeedbackEligibility(
            RestClient.Builder restClientBuilder,
            @Value("${group7-feedback.base-url:}") String baseUrl,
            @Value("${group7-feedback.eligibility-path:/api/work-orders/by-request/{activityId}}") String eligibilityPath) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.eligibilityPath = eligibilityPath == null || eligibilityPath.isBlank()
                ? "/api/work-orders/by-request/{activityId}" : eligibilityPath;
    }

    @Override
    public boolean canSubmit(String respondentId, FeedbackForm form) {
        // Sprint 2 synthetic mode remains available until Group 7 publishes its endpoint.
        if (baseUrl.isBlank()) return true;

        String uriTemplate = baseUrl + (eligibilityPath.startsWith("/") ? eligibilityPath : "/" + eligibilityPath);
        try {
            JsonNode response = restClient.get()
                    .uri(uriTemplate, form.getActivityId())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GROUP7_INVALID_ELIGIBILITY_RESPONSE");
            }
            JsonNode data = response.hasNonNull("data") ? response.get("data") : response;
            JsonNode eligible = data.get("eligible");
            if (eligible == null || !eligible.isBoolean()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GROUP7_INVALID_ELIGIBILITY_RESPONSE");
            }
            return eligible.booleanValue();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FEEDBACK_ACTIVITY_NOT_FOUND", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GROUP7_ELIGIBILITY_UNAVAILABLE", exception);
        }
    }
}
