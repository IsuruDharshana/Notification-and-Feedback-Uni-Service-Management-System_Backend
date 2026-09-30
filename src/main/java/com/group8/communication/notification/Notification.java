package com.group8.communication.notification;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient_read_created", columnList = "recipient_id,is_read,created_at"),
        @Index(name = "uk_notifications_idempotency_key", columnList = "idempotency_key", unique = true)
})
public class Notification {
    @Id @Column(columnDefinition = "char(36)") @JdbcTypeCode(SqlTypes.CHAR) private UUID id;
    @Column(name = "recipient_id", nullable = false, length = 64) private String recipientId;
    @Enumerated(EnumType.STRING) @Column(name = "notification_type", nullable = false, length = 64) private NotificationType type;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @Enumerated(EnumType.STRING) @Column(name = "related_type", nullable = false, length = 20) private RelatedType relatedType;
    @Column(name = "related_id", length = 128) private String relatedId;
    @Column(name = "source_service", nullable = false, length = 64) private String sourceService;
    @Column(name = "idempotency_key", nullable = false, length = 200, unique = true) private String idempotencyKey;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected Notification() {}
    public Notification(String recipientId, NotificationType type, String message, RelatedType relatedType, String relatedId,
                        String sourceService, String idempotencyKey) {
        this.id = UUID.randomUUID(); this.recipientId = recipientId; this.type = type; this.message = message;
        this.relatedType = relatedType; this.relatedId = relatedId; this.sourceService = sourceService;
        this.idempotencyKey = idempotencyKey; this.read = false; this.createdAt = LocalDateTime.now();
    }
    public Notification(String recipientId, NotificationType type, String message, RelatedType relatedType, UUID relatedId,
                        String sourceService, String idempotencyKey) {
        this(recipientId, type, message, relatedType, relatedId == null ? null : relatedId.toString(), sourceService, idempotencyKey);
    }
    public Notification(UUID recipientId, String message, RelatedType relatedType, UUID relatedId) {
        this(recipientId == null ? null : recipientId.toString(), NotificationType.LEGACY, message, relatedType, relatedId,
                "legacy", "legacy-" + UUID.randomUUID());
    }
    public UUID getId() { return id; } public String getRecipientId() { return recipientId; } public NotificationType getType() { return type; }
    public String getMessage() { return message; } public RelatedType getRelatedType() { return relatedType; } public String getRelatedId() { return relatedId; }
    public String getSourceService() { return sourceService; } public String getIdempotencyKey() { return idempotencyKey; }
    public boolean isRead() { return read; } public LocalDateTime getCreatedAt() { return createdAt; } public void markRead() { read = true; }
}
