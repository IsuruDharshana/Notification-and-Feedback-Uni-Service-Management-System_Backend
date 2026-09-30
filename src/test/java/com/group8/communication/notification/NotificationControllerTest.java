package com.group8.communication.notification;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {
    private final NotificationService service = mock(NotificationService.class);
    private final NotificationController controller = new NotificationController(service, "test-service-key");

    @Test
    void triggerRequiresTheConfiguredServiceKey() {
        NotificationDtos.TriggerRequest request = new NotificationDtos.TriggerRequest(
                "usr-student-001", NotificationType.EVENT_UPDATED, "Event changed", RelatedType.EVENT,
                UUID.randomUUID().toString(), "event-service", "event-001-updated");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.trigger("wrong-key", request));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void triggerDelegatesToServiceWithValidServiceKey() {
        Notification notification = new Notification("usr-student-001", NotificationType.EVENT_UPDATED,
                "Event changed", RelatedType.EVENT, UUID.randomUUID(), "event-service", "event-001-updated");
        NotificationDtos.TriggerRequest request = new NotificationDtos.TriggerRequest(
                notification.getRecipientId(), notification.getType(), notification.getMessage(),
                notification.getRelatedType(), notification.getRelatedId(), notification.getSourceService(),
                notification.getIdempotencyKey());
        when(service.createWithIdempotency(request)).thenReturn(new NotificationService.CreateResult(notification, false));

        ResponseEntity<NotificationDtos.Response> response = controller.trigger("test-service-key", request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(notification.getId(), response.getBody().id());
        verify(service).createWithIdempotency(request);
    }

    @Test
    void mineUsesAuthenticatedUserAndCapsPageSize() {
        String userId = "usr-student-001";
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        when(service.findMine(eq(userId), eq(false), eq(2), eq(100)))
                .thenReturn(new PageImpl<>(List.of()));

        controller.mine(authentication, false, 2, 500);

        verify(service).findMine(userId, false, 2, 100);
    }
}
