package com.group8.communication.notification;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record TriggerRequest(
            @NotBlank @Size(max = 64) String recipientId,
            @NotNull NotificationType type,
            @NotBlank @Size(max = 2000) String message,
            @NotNull RelatedType relatedType,
            @Size(max = 128) String relatedId,
            @NotBlank @Size(max = 64) String sourceService,
            @NotBlank @Size(max = 200) String idempotencyKey) {}
    public record Response(UUID id, String recipientId, NotificationType type, String message, RelatedType relatedType,
                           String relatedId, String sourceService, String idempotencyKey, boolean isRead, LocalDateTime createdAt) {
        static Response from(Notification n) { return new Response(n.getId(), n.getRecipientId(), n.getType(), n.getMessage(), n.getRelatedType(),
                n.getRelatedId(), n.getSourceService(), n.getIdempotencyKey(), n.isRead(), n.getCreatedAt()); }
    }
}
