package com.group8.communication.announcement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "Announcements", description = "Draft, publish and archive announcements, and view the caller's audience.")
@ApiResponses({
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
public class AnnouncementController {
    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVE_STAFF')")
    @Operation(operationId = "createAnnouncement", summary = "Create an announcement draft",
            description = "Requires ADMIN or ADMINISTRATIVE_STAFF. ALL must omit ruleValue; targeted audiences require it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft created.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    })
    public AnnouncementDtos.Response create(@Parameter(hidden = true) Authentication auth, @Valid @RequestBody AnnouncementDtos.CreateRequest request) {
        return AnnouncementDtos.Response.from(service.create(request, userId(auth)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVE_STAFF')")
    @Operation(operationId = "publishAnnouncement", summary = "Publish an owned draft",
            description = "Requires ADMIN or ADMINISTRATIVE_STAFF and ownership of the draft.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published announcement.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public AnnouncementDtos.Response publish(@PathVariable UUID id, @Parameter(hidden = true) Authentication auth) {
        return service.publish(id, userId(auth));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVE_STAFF')")
    @Operation(operationId = "archiveAnnouncement", summary = "Archive an owned published announcement",
            description = "Requires ADMIN or ADMINISTRATIVE_STAFF and ownership of the published announcement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archived announcement.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public AnnouncementDtos.Response archive(@PathVariable UUID id, @Parameter(hidden = true) Authentication auth) {
        return service.archive(id, userId(auth));
    }

    @GetMapping
    @Operation(operationId = "listVisibleAnnouncements", summary = "List published announcements visible to the caller",
            description = "Uses Group 5 roles and affiliation. When Group 5 is unconfigured or unavailable, "
                    + "only ALL and ROLE announcements are considered using the JWT roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visible announcements.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    public List<AnnouncementDtos.Response> visible(@Parameter(hidden = true) Authentication auth) {
        return service.visibleForUser(userId(auth), auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .toList());
    }

    private String userId(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_USER_ID");
        }
        return auth.getName();
    }
}
