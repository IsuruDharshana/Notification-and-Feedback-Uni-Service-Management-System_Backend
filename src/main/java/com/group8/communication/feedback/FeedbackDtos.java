package com.group8.communication.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    public record CreateFormRequest(
            @NotNull FeedbackActivityType activityType,
            @NotBlank @Size(max = 64) String activityId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20000) String questionsJson) {}

    public record SubmitResponseRequest(
            @Min(1) @Max(5) int rating,
            @Size(max = 2000) String comment) {}

    public record FormResponse(
            UUID id,
            FeedbackActivityType activityType,
            String activityId,
            String createdBy,
            String title,
            String questionsJson,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        static FormResponse from(FeedbackForm form) {
            return new FormResponse(form.getId(), form.getActivityType(), form.getActivityId(), form.getCreatedBy(), form.getTitle(),
                    form.getQuestionsJson(), form.isActive(), form.getCreatedAt(), form.getUpdatedAt());
        }
    }

    public record ResponseItem(
            UUID id,
            UUID formId,
            String activityId,
            String respondentId,
            int rating,
            String comment,
            LocalDateTime createdAt) {
        static ResponseItem from(FeedbackResponse response) {
            return new ResponseItem(response.getId(), response.getFormId(), response.getActivityId(),
                    response.getRespondentId(), response.getRating(), response.getComment(), response.getCreatedAt());
        }
    }
}
