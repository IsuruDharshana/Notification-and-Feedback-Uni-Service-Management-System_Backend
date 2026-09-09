package com.group8.communication.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_recipient_read_created", columnList = "recipient_id,is_read,created_at"))
public class Notification {
    @Id @Column(columnDefinition = "char(36)") private UUID id;
    @Column(name = "recipient_id", nullable = false, columnDefinition = "char(36)") private UUID recipientId;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @Enumerated(EnumType.STRING) @Column(name = "related_type", nullable = false, length = 20) private RelatedType relatedType;
    @Column(name = "related_id", columnDefinition = "char(36)") private UUID relatedId;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected Notification() {}
    public Notification(UUID recipientId, String message, RelatedType relatedType, UUID relatedId) {
        this.id = UUID.randomUUID(); this.recipientId = recipientId; this.message = message;
        this.relatedType = relatedType; this.relatedId = relatedId; this.read = false; this.createdAt = LocalDateTime.now();
    }
    public UUID getId() { return id; } public UUID getRecipientId() { return recipientId; } public String getMessage() { return message; }
    public RelatedType getRelatedType() { return relatedType; } public UUID getRelatedId() { return relatedId; }
    public boolean isRead() { return read; } public LocalDateTime getCreatedAt() { return createdAt; } public void markRead() { read = true; }
}
