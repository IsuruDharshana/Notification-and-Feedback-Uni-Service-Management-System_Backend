package com.group8.communication.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "Forms and one feedback response per user, form and activity.")
@ApiResponses({
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
public class FeedbackController {
    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping("/forms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ACADEMIC_STAFF', 'ADMIN', 'ADMINISTRATIVE_STAFF')")
    @Operation(operationId = "createFeedbackForm", summary = "Create a feedback form for an activity",
            description = "Requires EVENT_ORGANIZER, ACADEMIC_STAFF, ADMIN or ADMINISTRATIVE_STAFF. "
                    + "One form is allowed per activity type/id. Send questionsJson as an escaped JSON string.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback form created.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public FeedbackDtos.FormResponse createForm(@Parameter(hidden = true) Authentication auth, @Valid @RequestBody FeedbackDtos.CreateFormRequest request) {
        return service.createForm(request, userId(auth));
    }

    @GetMapping("/forms")
    @Operation(operationId = "listFeedbackForms", summary = "List active feedback forms", description = "Newest first.")
    @ApiResponse(responseCode = "200", description = "Active forms.", useReturnTypeSchema = true)
    public List<FeedbackDtos.FormResponse> listForms() {
        return service.listActiveForms();
    }

    @GetMapping("/forms/{formId}")
    @Operation(operationId = "getFeedbackForm", summary = "Get a feedback form")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback form.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public FeedbackDtos.FormResponse getForm(@PathVariable UUID formId) {
        return service.getForm(formId);
    }

    @PostMapping("/forms/{formId}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "submitFeedbackResponse", summary = "Submit a rating and optional comment",
            description = "Rating must be 1–5. Duplicate submissions return 409. Service-request feedback requires "
                    + "the caller to own a RESOLVED or CLOSED Group 7 request; event feedback requires a CONFIRMED "
                    + "event registration. The caller's bearer token is forwarded to the applicable provider. "
                    + "In development, an unconfigured provider URL skips its eligibility check.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback response saved.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/ServiceUnavailable")
    })
    public FeedbackDtos.ResponseItem submit(
            @PathVariable UUID formId,
            @Parameter(hidden = true) Authentication auth,
            @Valid @RequestBody FeedbackDtos.SubmitResponseRequest request) {
        return service.submit(formId, userId(auth), request);
    }

    @GetMapping("/forms/{formId}/responses")
    @Operation(operationId = "listFeedbackResponses", summary = "List responses to an owned form",
            description = "Only the user who created the form may read its responses. Newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback responses.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public List<FeedbackDtos.ResponseItem> listResponses(@PathVariable UUID formId, @Parameter(hidden = true) Authentication auth) {
        return service.listResponses(formId, userId(auth));
    }

    private String userId(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_USER_ID");
        }
        return auth.getName();
    }
}
