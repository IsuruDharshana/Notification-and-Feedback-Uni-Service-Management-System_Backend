package com.group8.communication.notification;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    public NotificationService(NotificationRepository repository) { this.repository = repository; }
    public Notification create(NotificationDtos.TriggerRequest request) {
        return repository.save(new Notification(request.recipientId(), request.message().trim(), request.relatedType(), request.relatedId()));
    }
    public Page<NotificationDtos.Response> findMine(UUID userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = unreadOnly ? repository.findByRecipientIdAndReadFalse(userId, pageable) : repository.findByRecipientId(userId, pageable);
        return result.map(NotificationDtos.Response::from);
    }
    public NotificationDtos.Response markRead(UUID id, UUID userId) {
        Notification notification = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND"));
        if (!notification.getRecipientId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOTIFICATION_FORBIDDEN");
        notification.markRead(); return NotificationDtos.Response.from(repository.save(notification));
    }
}
