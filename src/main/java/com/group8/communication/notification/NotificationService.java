package com.group8.communication.notification;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final RecipientDirectory recipientDirectory;
    public NotificationService(NotificationRepository repository, RecipientDirectory recipientDirectory) {
        this.repository = repository;
        this.recipientDirectory = recipientDirectory;
    }
    public CreateResult createWithIdempotency(NotificationDtos.TriggerRequest request) {
        if (request == null || request.recipientId() == null || request.recipientId().isBlank()
                || request.type() == null || request.relatedType() == null || request.message() == null || request.message().isBlank()
                || request.sourceService() == null || request.sourceService().isBlank()
                || request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_REQUEST");
        }
        if (request.type() == NotificationType.LEGACY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_TYPE");
        }
        Notification existing = repository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
        if (existing != null) {
            return new CreateResult(existing, true);
        }
        if (request.relatedType() != RelatedType.EXTERNAL && request.relatedId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RELATED_ID_REQUIRED");
        }
        if (!recipientDirectory.exists(request.recipientId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NOTIFICATION_RECIPIENT_NOT_FOUND");
        }
        Notification notification = new Notification(request.recipientId().trim(), request.type(), request.message().trim(), request.relatedType(),
                request.relatedId() == null ? null : request.relatedId().trim(), request.sourceService().trim(), request.idempotencyKey().trim());
        return new CreateResult(repository.save(notification), false);
    }
    public Notification create(NotificationDtos.TriggerRequest request) {
        return createWithIdempotency(request).notification();
    }
    public Page<NotificationDtos.Response> findMine(String userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = unreadOnly ? repository.findByRecipientIdAndReadFalse(userId, pageable) : repository.findByRecipientId(userId, pageable);
        return result.map(NotificationDtos.Response::from);
    }
    public NotificationDtos.Response markRead(UUID id, String userId) {
        Notification notification = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND"));
        if (!notification.getRecipientId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOTIFICATION_FORBIDDEN");
        notification.markRead(); return NotificationDtos.Response.from(repository.save(notification));
    }
    public record CreateResult(Notification notification, boolean replayed) {}
}
