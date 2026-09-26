package com.group8.communication.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class HttpRecipientDirectory implements RecipientDirectory {
    private final RestClient restClient;
    private final String baseUrl;
    private final String userPath;

    public HttpRecipientDirectory(
            RestClient.Builder restClientBuilder,
            @Value("${user-directory.base-url:}") String baseUrl,
            @Value("${user-directory.user-path:/api/users/{userId}}") String userPath) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.userPath = userPath == null || userPath.isBlank() ? "/api/users/{userId}" : userPath;
    }

    @Override
    public boolean exists(UUID recipientId) {
        if (recipientId == null) {
            return false;
        }
        if (baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_NOT_CONFIGURED");
        }

        String uriTemplate = baseUrl + (userPath.startsWith("/") ? userPath : "/" + userPath);
        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(uriTemplate, recipientId)
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound ignored) {
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RECIPIENT_DIRECTORY_UNAVAILABLE", exception);
        }
    }
}
