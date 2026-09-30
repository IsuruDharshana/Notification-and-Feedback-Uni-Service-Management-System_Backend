package com.group8.communication.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.group8.communication.integration.UserDirectory;
import com.group8.communication.integration.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
    private final String accessToken;

    public HttpRecipientDirectory(
            RestClient.Builder restClientBuilder,
            @Value("${user-directory.base-url:}") String baseUrl,
            @Value("${user-directory.user-path:/api/v1/validation/users/{userId}}") String userPath,
            @Value("${user-directory.eligibility-path:/api/v1/validation/users/{userId}/eligibility}") String eligibilityPath,
            @Value("${user-directory.required-role:}") String requiredRole,
            @Value("${user-directory.access-token:}") String accessToken) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.userPath = userPath == null || userPath.isBlank() ? "/api/v1/validation/users/{userId}" : userPath;
        this.eligibilityPath = eligibilityPath == null || eligibilityPath.isBlank()
                ? "/api/v1/validation/users/{userId}/eligibility" : eligibilityPath;
        this.requiredRole = requiredRole == null ? "" : requiredRole.trim();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    @Override
    public boolean exists(String recipientId) {
        if (recipientId == null) return false;
        if (baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_NOT_CONFIGURED");
        }
        String uriTemplate = eligibilityUriTemplate();
        try {
            RestClient.RequestHeadersSpec<?> request = requiredRole.isBlank()
                    ? restClient.get().uri(uriTemplate, recipientId)
                    : restClient.get().uri(uriTemplate, recipientId, requiredRole);
            JsonNode response = withAuthorization(request).retrieve().body(JsonNode.class);
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
            JsonNode response = withAuthorization(restClient.get().uri(uriTemplate, userId))
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.of(toUserProfile(response, userId));
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

    private UserProfile toUserProfile(JsonNode response, String fallbackId) {
        if (response == null || response.isNull()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_INVALID_RESPONSE");
        }
        JsonNode data = response.hasNonNull("data") ? response.get("data") : response;
        String id = firstText(data, "id", "user_id", "userId");
        if (id == null || id.isBlank()) id = fallbackId;
        String role = firstText(data, "role");
        if ((role == null || role.isBlank()) && data.path("roles").isArray() && data.path("roles").size() > 0) {
            role = data.path("roles").get(0).asText();
        }
        return new UserProfile(
                id,
                role,
                firstText(data, "department", "department_id", "departmentCode"),
                firstText(data, "faculty", "faculty_id", "facultyCode"),
                firstText(data, "serviceUnit", "service_unit", "serviceUnitCode"));
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private RestClient.RequestHeadersSpec<?> withAuthorization(RestClient.RequestHeadersSpec<?> request) {
        String token = incomingBearerToken();
        if (token.isBlank()) token = accessToken;
        if (token.isBlank()) return request;
        return request.header(HttpHeaders.AUTHORIZATION,
                token.startsWith("Bearer ") ? token : "Bearer " + token);
    }

    private String incomingBearerToken() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String header = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) return header;
        }
        return "";
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
