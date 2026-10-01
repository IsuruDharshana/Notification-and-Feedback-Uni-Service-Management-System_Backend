package com.group8.communication.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Recipient inbox, read state and trusted-service triggers.")
@ApiResponses({
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
public class NotificationController {
    private final NotificationService service; private final String serviceKey;
    public NotificationController(NotificationService service, @Value("${notification.service-key}") String serviceKey) { this.service = service; this.serviceKey = serviceKey; }
    @GetMapping
    @Operation(operationId = "listMyNotifications", summary = "List the signed-in user's notifications",
            description = "Newest first. The recipient is taken from the JWT subject; callers cannot select another user. "
                    + "Page numbers below zero become zero; page size is clamped to 1–100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of notifications.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    public Page<NotificationDtos.Response> mine(@Parameter(hidden = true) Authentication auth, @RequestParam(defaultValue="false") boolean unreadOnly, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        return service.findMine(userId(auth), unreadOnly, page, Math.min(size, 100));
    }
    @PatchMapping("/{id}/read")
    @Operation(operationId = "markNotificationRead", summary = "Mark an owned notification as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated notification.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public NotificationDtos.Response read(@PathVariable UUID id, @Parameter(hidden = true) Authentication auth) { return service.markRead(id, userId(auth)); }

    @PostMapping("/trigger")
    @Operation(operationId = "triggerNotification", summary = "Create an in-app notification from a trusted service",
            description = "Authorize with serviceKey (X-Service-Key). New keys create a record (201); "
                    + "repeated idempotency keys return the original record (200), even if the new payload differs. "
                    + "Use a distinct key per recipient and event. Group 5 recipient verification runs when a directory URL "
                    + "and credential are available. The current implementation accepts recipients unverified when those "
                    + "settings are missing or Group 5 rejects the credential. Other directory failures return 503.",
            security = @SecurityRequirement(name = "serviceKey"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification created.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "200", description = "Idempotent replay; original notification.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidServiceKey"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/ServiceUnavailable")
    })
    public ResponseEntity<NotificationDtos.Response> trigger(@Parameter(hidden = true) @RequestHeader(value="X-Service-Key", required=false) String key, @Valid @RequestBody NotificationDtos.TriggerRequest request) {
        if (serviceKey == null || serviceKey.isBlank() || !serviceKey.equals(key)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_SERVICE_KEY");
        NotificationService.CreateResult result = service.createWithIdempotency(request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(NotificationDtos.Response.from(result.notification()));
    }
    private String userId(Authentication auth) { try { return auth.getName(); } catch (Exception e) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_USER_ID"); } }
}
