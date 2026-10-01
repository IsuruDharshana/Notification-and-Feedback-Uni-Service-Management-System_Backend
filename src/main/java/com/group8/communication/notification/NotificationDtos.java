package com.group8.communication.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}
    @Schema(name = "NotificationTriggerRequest")
    public record TriggerRequest(
            @Schema(description = "Group 5 user id; not a UUID.", example = "usr-student-001")
            @NotBlank @Size(max = 64) String recipientId,
            @Schema(implementation = String.class, example = "EVENT_CANCELLED", allowableValues = {
                    "REGISTRATION_CONFIRMED", "REGISTRATION_CANCELLED", "EVENT_CANCELLED", "EVENT_UPDATED",
                    "RESERVATION_STATUS", "SERVICE_REQUEST_STATUS"})
            @NotNull NotificationType type,
            @Schema(example = "Your event has been cancelled.")
            @NotBlank @Size(max = 2000) String message,
            @Schema(example = "EVENT")
            @NotNull RelatedType relatedType,
            @Schema(description = "External string id. Required unless relatedType is EXTERNAL; only EXTERNAL may use null.",
                    example = "event-123", nullable = true)
            @Size(max = 128) String relatedId,
            @Schema(example = "event-service")
            @NotBlank @Size(max = 64) String sourceService,
            @Schema(description = "Globally unique key per intended notification. Reusing a key returns the original record.",
                    example = "event-123-cancelled-usr-student-001")
            @NotBlank @Size(max = 200) String idempotencyKey) {}
    @Schema(name = "Notification", description = "Persisted in-app notification.")
    public record Response(UUID id, String recipientId, NotificationType type, String message, RelatedType relatedType,
                           @Schema(nullable = true) String relatedId, String sourceService, String idempotencyKey, boolean isRead, LocalDateTime createdAt) {
        static Response from(Notification n) { return new Response(n.getId(), n.getRecipientId(), n.getType(), n.getMessage(), n.getRelatedType(),
                n.getRelatedId(), n.getSourceService(), n.getIdempotencyKey(), n.isRead(), n.getCreatedAt()); }
    }
}
