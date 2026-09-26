package com.group8.communication.feedback;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping("/forms")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDtos.FormResponse createForm(Authentication auth, @Valid @RequestBody FeedbackDtos.CreateFormRequest request) {
        return service.createForm(request, userId(auth));
    }

    @GetMapping("/forms")
    public List<FeedbackDtos.FormResponse> listForms() {
        return service.listActiveForms();
    }

    @GetMapping("/forms/{formId}")
    public FeedbackDtos.FormResponse getForm(@PathVariable UUID formId) {
        return service.getForm(formId);
    }

    @PostMapping("/forms/{formId}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDtos.ResponseItem submit(
            @PathVariable UUID formId,
            Authentication auth,
            @Valid @RequestBody FeedbackDtos.SubmitResponseRequest request) {
        return service.submit(formId, userId(auth), request);
    }

    @GetMapping("/forms/{formId}/responses")
    public List<FeedbackDtos.ResponseItem> listResponses(@PathVariable UUID formId, Authentication auth) {
        return service.listResponses(formId, userId(auth));
    }

    private UUID userId(Authentication auth) {
        try {
            return UUID.fromString(auth.getName());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_USER_ID");
        }
    }
}
