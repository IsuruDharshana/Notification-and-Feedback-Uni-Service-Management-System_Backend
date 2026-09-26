package com.group8.communication.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AnnouncementDtos {
    private AnnouncementDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @NotNull AudienceType audienceType,
            @Size(max = 100) String ruleValue) {}

    public record Response(
            UUID id,
            String title,
            String content,
            AnnouncementStatus status,
            UUID createdBy,
            AudienceType audienceType,
            String ruleValue,
            LocalDateTime publishedAt,
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
