package com.group8.communication.notification;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service; private final String serviceKey;
    public NotificationController(NotificationService service, @Value("${notification.service-key}") String serviceKey) { this.service = service; this.serviceKey = serviceKey; }
    @GetMapping public Page<NotificationDtos.Response> mine(Authentication auth, @RequestParam(defaultValue="false") boolean unreadOnly, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        return service.findMine(userId(auth), unreadOnly, page, Math.min(size, 100));
    }
    @PatchMapping("/{id}/read") public NotificationDtos.Response read(@PathVariable UUID id, Authentication auth) { return service.markRead(id, userId(auth)); }
    @PostMapping("/trigger") @ResponseStatus(HttpStatus.CREATED) public NotificationDtos.Response trigger(@RequestHeader(value="X-Service-Key", required=false) String key, @Valid @RequestBody NotificationDtos.TriggerRequest request) {
        if (serviceKey == null || serviceKey.isBlank() || !serviceKey.equals(key)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "INVALID_SERVICE_KEY");
        return NotificationDtos.Response.from(service.create(request));
    }
    private UUID userId(Authentication auth) { try { return UUID.fromString(auth.getName()); } catch (Exception e) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_USER_ID"); } }
}
