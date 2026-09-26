package com.group8.communication.announcement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementDtos.Response create(Authentication auth, @Valid @RequestBody AnnouncementDtos.CreateRequest request) {
        return AnnouncementDtos.Response.from(service.create(request, userId(auth)));
    }

    @PostMapping("/{id}/publish")
    public AnnouncementDtos.Response publish(@PathVariable UUID id, Authentication auth) {
        return service.publish(id, userId(auth));
    }

    @PostMapping("/{id}/archive")
    public AnnouncementDtos.Response archive(@PathVariable UUID id, Authentication auth) {
        return service.archive(id, userId(auth));
    }

    @GetMapping
    public List<AnnouncementDtos.Response> visible(Authentication auth) {
        return service.visibleForUser(userId(auth));
    }

    private UUID userId(Authentication auth) {
        try {
            return UUID.fromString(auth.getName());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_USER_ID");
        }
    }
}
