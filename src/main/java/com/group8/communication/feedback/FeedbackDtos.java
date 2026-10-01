package com.group8.communication.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    @Schema(name = "FeedbackFormCreateRequest")
    public record CreateFormRequest(
            @Schema(example = "SERVICE_REQUEST")
            @NotNull FeedbackActivityType activityType,
            @Schema(description = "Event id or Group 7 service request id; not restricted to UUIDs.", example = "REQ-2026-004")
            @NotBlank @Size(max = 64) String activityId,
            @Schema(example = "Service request feedback")
            @NotBlank @Size(max = 200) String title,
            @Schema(description = "Question definition encoded as a JSON string, not a nested JSON array.",
                    example = "[{\"type\":\"rating\",\"label\":\"How was the service?\"}]")
            @NotBlank @Size(max = 20000) String questionsJson) {}

    @Schema(name = "FeedbackResponseRequest")
    public record SubmitResponseRequest(
            @Schema(example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
            @Min(1) @Max(5) int rating,
            @Schema(example = "Resolved quickly.", nullable = true)
            @Size(max = 2000) String comment) {}

    @Schema(name = "FeedbackForm")
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

    @Schema(name = "FeedbackResponse")
    public record ResponseItem(
            UUID id,
            UUID formId,
            String activityId,
            String respondentId,
            int rating,
            @Schema(nullable = true) String comment,
            LocalDateTime createdAt) {
        static ResponseItem from(FeedbackResponse response) {
            return new ResponseItem(response.getId(), response.getFormId(), response.getActivityId(),
                    response.getRespondentId(), response.getRating(), response.getComment(), response.getCreatedAt());
        }
    }
}
