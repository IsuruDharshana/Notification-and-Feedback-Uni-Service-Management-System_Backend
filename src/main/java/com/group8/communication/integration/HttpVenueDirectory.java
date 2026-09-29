package com.group8.communication.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class HttpVenueDirectory implements VenueDirectory {
    private final RestClient restClient;
    private final String baseUrl;
    private final String venuePath;

    public HttpVenueDirectory(
            RestClient.Builder restClientBuilder,
            @Value("${facility-directory.base-url:}") String baseUrl,
            @Value("${facility-directory.venue-path:/api/facilities/{venueId}}") String venuePath) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.venuePath = venuePath == null || venuePath.isBlank() ? "/api/facilities/{venueId}" : venuePath;
    }

    @Override
    public boolean exists(UUID venueId) {
        if (venueId == null) return false;
        if (baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FACILITY_DIRECTORY_NOT_CONFIGURED");
        }
        String uriTemplate = baseUrl + (venuePath.startsWith("/") ? venuePath : "/" + venuePath);
        try {
            restClient.get().uri(uriTemplate, venueId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ignored) {
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FACILITY_DIRECTORY_UNAVAILABLE", exception);
        }
    }
}
