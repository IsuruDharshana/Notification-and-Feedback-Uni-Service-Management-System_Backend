package com.group8.communication.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.group8.communication.integration.UserDirectory;
import com.group8.communication.integration.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class HttpRecipientDirectory implements RecipientDirectory, UserDirectory {
    private final RestClient restClient;
    private final String baseUrl;
    private final String userPath;
    private final String eligibilityPath;
    private final String requiredRole;

    public HttpRecipientDirectory(
            RestClient.Builder restClientBuilder,
            @Value("${user-directory.base-url:}") String baseUrl,
            @Value("${user-directory.user-path:/api/users/{userId}}") String userPath,
            @Value("${user-directory.eligibility-path:/api/v1/validation/users/{userId}/eligibility}") String eligibilityPath,
            @Value("${user-directory.required-role:}") String requiredRole) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.userPath = userPath == null || userPath.isBlank() ? "/api/users/{userId}" : userPath;
        this.eligibilityPath = eligibilityPath == null || eligibilityPath.isBlank()
                ? "/api/v1/validation/users/{userId}/eligibility" : eligibilityPath;
        this.requiredRole = requiredRole == null ? "" : requiredRole.trim();
    }

    @Override
    public boolean exists(String recipientId) {
        if (recipientId == null) return false;
        if (baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_NOT_CONFIGURED");
        }
        String uriTemplate = eligibilityUriTemplate();
        try {
            JsonNode response = requiredRole.isBlank()
                    ? restClient.get().uri(uriTemplate, recipientId).retrieve().body(JsonNode.class)
                    : restClient.get().uri(uriTemplate, recipientId, requiredRole).retrieve().body(JsonNode.class);
            return isEligible(response);
        } catch (HttpClientErrorException.NotFound ignored) {
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_UNAVAILABLE", exception);
        }
    }

    @Override
    public Optional<UserProfile> findById(String userId) {
        if (userId == null) return Optional.empty();
        if (baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_NOT_CONFIGURED");
        }

        String uriTemplate = baseUrl + (userPath.startsWith("/") ? userPath : "/" + userPath);
        try {
            UserProfile profile = restClient.get()
                    .uri(uriTemplate, userId)
                    .retrieve()
                    .body(UserProfile.class);
            return Optional.ofNullable(profile);
        } catch (HttpClientErrorException.NotFound ignored) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_UNAVAILABLE", exception);
        }
    }

    private String eligibilityUriTemplate() {
        String path = eligibilityPath.startsWith("/") ? eligibilityPath : "/" + eligibilityPath;
        if (requiredRole.isBlank()) {
            return baseUrl + path;
        }
        return baseUrl + path + (path.contains("?") ? "&" : "?") + "required_role={requiredRole}";
    }

    private boolean isEligible(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_INVALID_RESPONSE");
        }
        JsonNode data = response.hasNonNull("data") ? response.get("data") : response;
        JsonNode eligible = data.get("eligible");
        if (eligible == null || !eligible.isBoolean()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_INVALID_RESPONSE");
        }
        return eligible.booleanValue();
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
