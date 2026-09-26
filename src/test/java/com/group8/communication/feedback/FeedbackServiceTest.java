package com.group8.communication.feedback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {
    @Mock private FeedbackFormRepository formRepository;
    @Mock private FeedbackResponseRepository responseRepository;
    @Mock private FeedbackEligibility eligibility;
    @InjectMocks private FeedbackService service;

    @Test
    void createsFeedbackForm() {
        when(formRepository.findByActivityTypeAndActivityId(any(), any())).thenReturn(Optional.empty());
        when(formRepository.save(any(FeedbackForm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackDtos.FormResponse result = service.createForm(new FeedbackDtos.CreateFormRequest(
                FeedbackActivityType.EVENT, UUID.randomUUID(), "Event feedback", "[{\"type\":\"rating\"}]"), UUID.randomUUID());

        assertEquals("Event feedback", result.title());
        assertEquals(true, result.active());
        verify(formRepository).save(any(FeedbackForm.class));
    }

    @Test
    void preventsDuplicateResponseForSameUserAndActivity() {
        UUID formId = UUID.randomUUID();
        UUID respondentId = UUID.randomUUID();
        FeedbackForm form = new FeedbackForm(FeedbackActivityType.EVENT, UUID.randomUUID(), "Feedback", "[]", UUID.randomUUID());
        when(formRepository.findById(formId)).thenReturn(Optional.of(form));
        when(eligibility.canSubmit(respondentId, form)).thenReturn(true);
        when(responseRepository.existsByFormIdAndActivityIdAndRespondentId(formId, form.getActivityId(), respondentId))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.submit(
                formId, respondentId, new FeedbackDtos.SubmitResponseRequest(5, "Great")));

        assertEquals(409, exception.getStatusCode().value());
        verify(responseRepository, never()).save(any(FeedbackResponse.class));
    }

    @Test
    void submitsEligibleResponseWithFivePointRating() {
        UUID formId = UUID.randomUUID();
        UUID respondentId = UUID.randomUUID();
        FeedbackForm form = new FeedbackForm(FeedbackActivityType.SERVICE_REQUEST, UUID.randomUUID(), "Service feedback", "[]", UUID.randomUUID());
        when(formRepository.findById(formId)).thenReturn(Optional.of(form));
        when(eligibility.canSubmit(respondentId, form)).thenReturn(true);
        when(responseRepository.existsByFormIdAndActivityIdAndRespondentId(formId, form.getActivityId(), respondentId))
                .thenReturn(false);
        when(responseRepository.save(any(FeedbackResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackDtos.ResponseItem result = service.submit(
                formId, respondentId, new FeedbackDtos.SubmitResponseRequest(5, "Resolved quickly"));

        assertEquals(5, result.rating());
        assertEquals(respondentId, result.respondentId());
        verify(responseRepository).save(any(FeedbackResponse.class));
    }
}
