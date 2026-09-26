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
        UUID recipientId = UUID.randomUUID();
        NotificationDtos.TriggerRequest request = new NotificationDtos.TriggerRequest(
                recipientId, "  Reservation approved.  ", RelatedType.RESERVATION, UUID.randomUUID());
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.create(request);

        assertEquals(recipientId, result.getRecipientId());
        assertEquals("Reservation approved.", result.getMessage());
        assertEquals(RelatedType.RESERVATION, result.getRelatedType());
        assertFalse(result.isRead());
        verify(repository).save(any(Notification.class));
    }

    @Test
    void createRejectsInvalidRequestWhenCalledOutsideTheController() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(new NotificationDtos.TriggerRequest(
                        UUID.randomUUID(), "   ", RelatedType.EVENT, null)));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsUnknownRecipientBeforePersisting() {
        UUID recipientId = UUID.randomUUID();
        when(recipientDirectory.exists(recipientId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(new NotificationDtos.TriggerRequest(recipientId, "Message", RelatedType.EVENT, UUID.randomUUID())));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("NOTIFICATION_RECIPIENT_NOT_FOUND", exception.getReason());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findMineUsesUnreadRepositoryAndNewestFirstOrdering() {
        UUID recipientId = UUID.randomUUID();
        Notification notification = new Notification(recipientId, "Unread", RelatedType.EVENT, null);
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
        UUID userId = UUID.randomUUID();
        Notification notification = new Notification(userId, "Please review", RelatedType.SERVICE_REQUEST, UUID.randomUUID());
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        NotificationDtos.Response result = service.markRead(notification.getId(), userId);

        assertTrue(notification.isRead());
        assertTrue(result.isRead());
        verify(repository).save(notification);
    }

    @Test
    void markReadRejectsNotificationOwnedByAnotherUser() {
        Notification notification = new Notification(UUID.randomUUID(), "Private", RelatedType.ANNOUNCEMENT, null);
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markRead(notification.getId(), UUID.randomUUID()));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void markReadReturnsNotFoundForUnknownNotification() {
        UUID notificationId = UUID.randomUUID();
        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markRead(notificationId, UUID.randomUUID()));

        assertEquals(404, exception.getStatusCode().value());
    }
}
