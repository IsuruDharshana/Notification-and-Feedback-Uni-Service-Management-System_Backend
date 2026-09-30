package com.group8.communication.notification;

import com.group8.communication.integration.UserDirectory;
import com.group8.communication.integration.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Group 5 directory client, using {@code GET /api/v1/validation/users/{userId}/eligibility}, which
 * returns the user's roles and department/faculty affiliation. Group 5 requires a bearer token: the
 * calling user's token is forwarded, or USER_DIRECTORY_ACCESS_TOKEN is used for service-to-service calls.
 */
@Component
public class HttpRecipientDirectory implements RecipientDirectory, UserDirectory {
    private static final Logger log = LoggerFactory.getLogger(HttpRecipientDirectory.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String eligibilityPath;
    private final String requiredRole;
    private final String accessToken;

    public HttpRecipientDirectory(
            RestClient.Builder restClientBuilder,
            @Value("${user-directory.base-url:}") String baseUrl,
            @Value("${user-directory.eligibility-path:/api/v1/validation/users/{userId}/eligibility}") String eligibilityPath,
            @Value("${user-directory.required-role:}") String requiredRole,
            @Value("${user-directory.access-token:}") String accessToken) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.eligibilityPath = eligibilityPath == null || eligibilityPath.isBlank()
                ? "/api/v1/validation/users/{userId}/eligibility" : eligibilityPath;
        this.requiredRole = requiredRole == null ? "" : requiredRole.trim();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }

    /**
     * Recipient check for notification triggers. The caller is already trusted through X-Service-Key,
     * so when Group 5 cannot be asked (no URL or no credential) or rejects our credential, the recipient
     * is accepted and a warning is logged instead of dropping the notification.
     */
    @Override
    public boolean exists(String recipientId) {
        if (recipientId == null) return false;
        String token = bearerToken();
        if (!isConfigured() || token.isBlank()) {
            log.warn("Recipient {} not verified with Group 5: set USER_DIRECTORY_BASE_URL and USER_DIRECTORY_ACCESS_TOKEN", recipientId);
            return true;
        }
        String uriTemplate = eligibilityUriTemplate(!requiredRole.isBlank());
        try {
            RestClient.RequestHeadersSpec<?> request = requiredRole.isBlank()
                    ? restClient.get().uri(uriTemplate, recipientId)
                    : restClient.get().uri(uriTemplate, recipientId, requiredRole);
            JsonNode data = dataOf(request.header(HttpHeaders.AUTHORIZATION, token).retrieve().body(JsonNode.class));
            JsonNode eligible = data.get("eligible");
            if (eligible == null || !eligible.isBoolean()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_INVALID_RESPONSE");
            }
            return eligible.booleanValue();
        } catch (HttpClientErrorException.NotFound ignored) {
            return false;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Group 5 rejected the directory credential ({}); recipient {} accepted unverified",
                    exception.getStatusCode(), recipientId);
            return true;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_UNAVAILABLE", exception);
        }
    }

    @Override
    public Optional<UserProfile> findById(String userId) {
        if (userId == null) return Optional.empty();
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_NOT_CONFIGURED");
        }
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(eligibilityUriTemplate(false), userId);
            String token = bearerToken();
            if (!token.isBlank()) request = request.header(HttpHeaders.AUTHORIZATION, token);
            return Optional.of(toUserProfile(dataOf(request.retrieve().body(JsonNode.class)), userId));
        } catch (HttpClientErrorException.NotFound ignored) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_UNAVAILABLE", exception);
        }
    }

    private String eligibilityUriTemplate(boolean withRequiredRole) {
        String path = eligibilityPath.startsWith("/") ? eligibilityPath : "/" + eligibilityPath;
        if (!withRequiredRole) return baseUrl + path;
        return baseUrl + path + (path.contains("?") ? "&" : "?") + "required_role={requiredRole}";
    }

    private static JsonNode dataOf(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_INVALID_RESPONSE");
        }
        return response.hasNonNull("data") ? response.get("data") : response;
    }

    private static UserProfile toUserProfile(JsonNode data, String fallbackId) {
        String id = firstText(data, "user_id", "userId", "id");
        List<String> roles = new ArrayList<>();
        JsonNode roleList = data.get("roles");
        if (roleList != null && roleList.isArray()) {
            for (JsonNode role : roleList) {
                if (role.isString() && !role.stringValue().isBlank()) roles.add(role.stringValue());
            }
        }
        JsonNode affiliation = data.hasNonNull("affiliation") ? data.get("affiliation") : data;
        return new UserProfile(
                id == null ? fallbackId : id,
                roles,
                firstText(affiliation, "department_id", "departmentId", "department"),
                firstText(affiliation, "faculty_id", "facultyId", "faculty"),
                firstText(affiliation, "service_unit_id", "serviceUnitId", "serviceUnit"));
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isString() && !value.stringValue().isBlank()) return value.stringValue();
        }
        return null;
    }

    /** The calling user's bearer token, else the configured service credential, else "". */
    private String bearerToken() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String header = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) return header;
        }
        if (accessToken.isBlank()) return "";
        return accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
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
