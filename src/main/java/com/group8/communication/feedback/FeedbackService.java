package com.group8.communication.feedback;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class FeedbackService {
    private final FeedbackFormRepository formRepository;
    private final FeedbackResponseRepository responseRepository;
    private final FeedbackEligibility eligibility;

    public FeedbackService(FeedbackFormRepository formRepository,
                           FeedbackResponseRepository responseRepository,
                           FeedbackEligibility eligibility) {
        this.formRepository = formRepository;
        this.responseRepository = responseRepository;
        this.eligibility = eligibility;
    }

    public FeedbackDtos.FormResponse createForm(FeedbackDtos.CreateFormRequest request, String createdBy) {
        if (createdBy == null || request == null || request.activityType() == null
                || request.activityId() == null || request.activityId().isBlank()
                || request.title() == null || request.title().isBlank()
                || request.questionsJson() == null || request.questionsJson().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_FEEDBACK_FORM_REQUEST");
        }
        String activityId = request.activityId().trim();
        if (formRepository.findByActivityTypeAndActivityId(request.activityType(), activityId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FEEDBACK_FORM_ALREADY_EXISTS");
        }
        FeedbackForm form = new FeedbackForm(request.activityType(), activityId,
                request.title().trim(), request.questionsJson().trim(), createdBy);
        return FeedbackDtos.FormResponse.from(formRepository.save(form));
    }

    public FeedbackDtos.FormResponse getForm(UUID formId) {
        return FeedbackDtos.FormResponse.from(findForm(formId));
    }

    public List<FeedbackDtos.FormResponse> listActiveForms() {
        return formRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(FeedbackDtos.FormResponse::from)
                .toList();
    }

    public FeedbackDtos.ResponseItem submit(UUID formId, String respondentId, FeedbackDtos.SubmitResponseRequest request) {
        if (respondentId == null || request == null || request.rating() < 1 || request.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_FEEDBACK_RESPONSE_REQUEST");
        }
        FeedbackForm form = findForm(formId);
        if (!form.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FEEDBACK_FORM_INACTIVE");
        }
        if (!eligibility.canSubmit(respondentId, form)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FEEDBACK_NOT_ELIGIBLE");
        }
        if (responseRepository.existsByFormIdAndActivityIdAndRespondentId(formId, form.getActivityId(), respondentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FEEDBACK_ALREADY_SUBMITTED");
        }
        String comment = request.comment() == null ? null : request.comment().trim();
        FeedbackResponse response = new FeedbackResponse(formId, form.getActivityId(), respondentId, request.rating(), comment);
        return FeedbackDtos.ResponseItem.from(responseRepository.save(response));
    }

    public List<FeedbackDtos.ResponseItem> listResponses(UUID formId, String actorId) {
        FeedbackForm form = findForm(formId);
        if (actorId == null || !form.getCreatedBy().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FEEDBACK_RESPONSES_FORBIDDEN");
        }
        return responseRepository.findByFormIdOrderByCreatedAtDesc(formId).stream()
                .map(FeedbackDtos.ResponseItem::from)
                .toList();
    }

    private FeedbackForm findForm(UUID formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FEEDBACK_FORM_NOT_FOUND"));
    }
}
