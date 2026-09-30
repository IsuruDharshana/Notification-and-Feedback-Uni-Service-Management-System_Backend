package com.group8.communication.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository repository;

    @Mock
    private RecipientDirectory recipientDirectory;

    @InjectMocks
    private NotificationService service;

    @BeforeEach
    void allowKnownRecipientsByDefault() {
        lenient().when(recipientDirectory.exists(any())).thenReturn(true);
    }

    @Test
    void createTrimsMessageAndPersistsNotification() {
        String recipientId = "usr-student-001";
        UUID relatedId = UUID.randomUUID();
        NotificationDtos.TriggerRequest request = new NotificationDtos.TriggerRequest(
                recipientId, NotificationType.REGISTRATION_CONFIRMED, "  Reservation approved.  ",
                RelatedType.REGISTRATION, relatedId.toString(), "event-service", "registration-001-confirmed");
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.create(request);

        assertEquals(recipientId, result.getRecipientId());
        assertEquals("Reservation approved.", result.getMessage());
        assertEquals(NotificationType.REGISTRATION_CONFIRMED, result.getType());
        assertEquals(RelatedType.REGISTRATION, result.getRelatedType());
        assertEquals("event-service", result.getSourceService());
        assertEquals("registration-001-confirmed", result.getIdempotencyKey());
        assertFalse(result.isRead());
        verify(repository).save(any(Notification.class));
    }

    @Test
    void createRejectsInvalidRequestWhenCalledOutsideTheController() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(new NotificationDtos.TriggerRequest(
                        "usr-student-001", NotificationType.EVENT_UPDATED, "   ", RelatedType.EVENT,
                        UUID.randomUUID().toString(), "event-service", "event-001-updated")));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsUnknownRecipientBeforePersisting() {
        String recipientId = "usr-student-404";
        when(recipientDirectory.exists(recipientId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(new NotificationDtos.TriggerRequest(
                        recipientId, NotificationType.EVENT_UPDATED, "Message", RelatedType.EVENT, UUID.randomUUID().toString(),
                        "event-service", "event-404-updated")));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("NOTIFICATION_RECIPIENT_NOT_FOUND", exception.getReason());
        verify(repository).findByIdempotencyKey("event-404-updated");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findMineUsesUnreadRepositoryAndNewestFirstOrdering() {
        String recipientId = "usr-student-001";
        Notification notification = new Notification(recipientId, NotificationType.EVENT_UPDATED, "Unread",
                RelatedType.EVENT, null, "event-service", "event-001-updated");
        when(repository.findByRecipientIdAndReadFalse(any(), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        var result = service.findMine(recipientId, true, -1, 500);

        assertEquals(1, result.getTotalElements());
        assertEquals("Unread", result.getContent().get(0).message());
        verify(repository).findByRecipientIdAndReadFalse(eq(recipientId), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void markReadOnlyUpdatesNotificationOwnedByUser() {
        String userId = "usr-student-001";
        Notification notification = new Notification(userId, NotificationType.EVENT_UPDATED, "Please review",
                RelatedType.SERVICE_REQUEST, UUID.randomUUID(), "event-service", "service-request-001-updated");
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        NotificationDtos.Response result = service.markRead(notification.getId(), userId);

        assertTrue(notification.isRead());
        assertTrue(result.isRead());
        verify(repository).save(notification);
    }

    @Test
    void markReadRejectsNotificationOwnedByAnotherUser() {
        Notification notification = new Notification("usr-student-002", NotificationType.EVENT_UPDATED, "Private",
                RelatedType.ANNOUNCEMENT, null, "event-service", "announcement-001-updated");
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markRead(notification.getId(), "usr-student-001"));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void markReadReturnsNotFoundForUnknownNotification() {
        UUID notificationId = UUID.randomUUID();
        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markRead(notificationId, "usr-student-001"));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void createReturnsExistingNotificationOnIdempotencyReplay() {
        String idempotencyKey = "event-001-updated";
        Notification existing = new Notification("usr-student-001", NotificationType.EVENT_UPDATED,
                "Already delivered", RelatedType.EVENT, UUID.randomUUID(), "event-service", idempotencyKey);
        NotificationDtos.TriggerRequest request = new NotificationDtos.TriggerRequest(
                "usr-student-001", NotificationType.EVENT_UPDATED, "Already delivered", RelatedType.EVENT,
                existing.getRelatedId(), "event-service", idempotencyKey);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        NotificationService.CreateResult result = service.createWithIdempotency(request);

        assertTrue(result.replayed());
        assertEquals(existing.getId(), result.notification().getId());
        verify(repository).findByIdempotencyKey(idempotencyKey);
    }
}
