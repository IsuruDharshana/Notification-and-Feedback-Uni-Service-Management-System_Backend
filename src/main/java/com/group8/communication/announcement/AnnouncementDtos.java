package com.group8.communication.announcement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AnnouncementDtos {
    private AnnouncementDtos() {}

    @Schema(name = "AnnouncementCreateRequest")
    public record CreateRequest(
            @Schema(example = "Library opening hours")
            @NotBlank @Size(max = 200) String title,
            @Schema(example = "The library will close at 6 pm on Friday.")
            @NotBlank String content,
            @Schema(example = "ROLE")
            @NotNull AudienceType audienceType,
            @Schema(description = "Required for a targeted audience; omit or use null for ALL. "
                    + "Use the agreed Group 5 role or affiliation code.", example = "STUDENT", nullable = true)
            @Size(max = 100) String ruleValue) {}

    @Schema(name = "Announcement")
    public record Response(
            UUID id,
            String title,
            String content,
            AnnouncementStatus status,
            String createdBy,
            AudienceType audienceType,
            @Schema(nullable = true) String ruleValue,
            @Schema(nullable = true) LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        static Response from(Announcement announcement) {
            return new Response(
                    announcement.getId(),
                    announcement.getTitle(),
                    announcement.getContent(),
                    announcement.getStatus(),
                    announcement.getCreatedBy(),
                    announcement.getAudienceRule().getAudienceType(),
                    announcement.getAudienceRule().getRuleValue(),
                    announcement.getPublishedAt(),
                    announcement.getCreatedAt(),
                    announcement.getUpdatedAt());
        }
    }
}
