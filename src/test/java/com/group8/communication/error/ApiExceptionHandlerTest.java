package com.group8.communication.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void exposesStableErrorCodeWithoutInternalExceptionDetails() {
        var response = handler.handleStatus(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "GROUP7_ELIGIBILITY_UNAVAILABLE", new IllegalStateException("secret details")));

        assertEquals(503, response.getStatusCode().value());
        assertEquals(new ApiError("GROUP7_ELIGIBILITY_UNAVAILABLE"), response.getBody());
    }

    @Test
    void hidesUnexpectedExceptionDetailsFromClient() {
        var response = handler.handleUnexpected(new IllegalStateException("sensitive internal detail"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals(new ApiError("INTERNAL_SERVER_ERROR"), response.getBody());
    }

    @Test
    void roleDenialIsForbiddenNotServerError() {
        var response = handler.handleAccessDenied();

        assertEquals(403, response.getStatusCode().value());
        assertEquals(new ApiError("FORBIDDEN"), response.getBody());
    }
}
