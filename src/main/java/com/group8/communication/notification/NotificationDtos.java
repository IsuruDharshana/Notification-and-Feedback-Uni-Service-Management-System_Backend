package com.group8.communication.notification;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record TriggerRequest(@NotNull UUID recipientId, @NotBlank String message, @NotNull RelatedType relatedType, UUID relatedId) {}
    public record Response(UUID id, UUID recipientId, String message, RelatedType relatedType, UUID relatedId, boolean isRead, LocalDateTime createdAt) {
        static Response from(Notification n) { return new Response(n.getId(), n.getRecipientId(), n.getMessage(), n.getRelatedType(), n.getRelatedId(), n.isRead(), n.getCreatedAt()); }
    }
}
