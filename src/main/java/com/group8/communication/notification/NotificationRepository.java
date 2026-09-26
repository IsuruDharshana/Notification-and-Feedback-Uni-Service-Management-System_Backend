package com.group8.communication.notification;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);
    Page<Notification> findByRecipientIdAndReadFalse(String recipientId, Pageable pageable);
    java.util.Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
