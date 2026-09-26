package com.group8.communication.notification;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
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
                UUID.randomUUID(), "Event changed", RelatedType.EVENT, UUID.randomUUID());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.trigger("wrong-key", request));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void triggerDelegatesToServiceWithValidServiceKey() {
        Notification notification = new Notification(UUID.randomUUID(), "Event changed", RelatedType.EVENT, UUID.randomUUID());
        NotificationDtos.TriggerRequest request = new NotificationDtos.TriggerRequest(
                notification.getRecipientId(), notification.getMessage(), notification.getRelatedType(), notification.getRelatedId());
        when(service.create(request)).thenReturn(notification);

        NotificationDtos.Response response = controller.trigger("test-service-key", request);

        assertEquals(notification.getId(), response.id());
        verify(service).create(request);
    }

    @Test
    void mineUsesAuthenticatedUserAndCapsPageSize() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId.toString());
        when(service.findMine(eq(userId), eq(false), eq(2), eq(100)))
                .thenReturn(new PageImpl<>(List.of()));

        controller.mine(authentication, false, 2, 500);

        verify(service).findMine(userId, false, 2, 100);
    }
}
