package com.group8.communication.announcement;

import com.group8.communication.integration.UserDirectory;
import com.group8.communication.integration.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {
    private final AnnouncementRepository repository;
    private final UserDirectory userDirectory;

    public AnnouncementService(AnnouncementRepository repository, UserDirectory userDirectory) {
        this.repository = repository;
        this.userDirectory = userDirectory;
    }

    public Announcement create(AnnouncementDtos.CreateRequest request, String createdBy) {
        if (createdBy == null || request == null || request.audienceType() == null
                || request.title() == null || request.title().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_ANNOUNCEMENT_REQUEST");
        }
        String ruleValue = normalizeRuleValue(request.audienceType(), request.ruleValue());
        return repository.save(new Announcement(
                request.title().trim(),
                request.content().trim(),
                createdBy,
                new AudienceRule(request.audienceType(), ruleValue)));
    }

    public AnnouncementDtos.Response publish(UUID id, String actorId) {
        Announcement announcement = find(id);
        verifyOwner(announcement, actorId);
        if (announcement.getStatus() != AnnouncementStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ANNOUNCEMENT_NOT_DRAFT");
        }
        announcement.publish(LocalDateTime.now());
        return AnnouncementDtos.Response.from(repository.save(announcement));
    }

    public AnnouncementDtos.Response archive(UUID id, String actorId) {
        Announcement announcement = find(id);
        verifyOwner(announcement, actorId);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ANNOUNCEMENT_NOT_PUBLISHED");
        }
        announcement.archive(LocalDateTime.now());
        return AnnouncementDtos.Response.from(repository.save(announcement));
    }

    public List<AnnouncementDtos.Response> visible(AudienceType audienceType, String audienceValue) {
        String normalizedValue = audienceValue == null ? null : audienceValue.trim();
        return repository.findByStatusOrderByPublishedAtDesc(AnnouncementStatus.PUBLISHED).stream()
                .filter(announcement -> isVisible(announcement.getAudienceRule(), audienceType, normalizedValue))
                .map(AnnouncementDtos.Response::from)
                .toList();
    }

    public List<AnnouncementDtos.Response> visibleForUser(String userId) {
        UserProfile profile = userDirectory.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        return repository.findByStatusOrderByPublishedAtDesc(AnnouncementStatus.PUBLISHED).stream()
                .filter(announcement -> isVisible(announcement.getAudienceRule(), profile))
                .map(AnnouncementDtos.Response::from)
                .toList();
    }

    private boolean isVisible(AudienceRule rule, AudienceType audienceType, String audienceValue) {
        if (rule.getAudienceType() == AudienceType.ALL) {
            return true;
        }
        return audienceType == rule.getAudienceType()
                && audienceValue != null
                && audienceValue.equalsIgnoreCase(rule.getRuleValue());
    }

    private boolean isVisible(AudienceRule rule, UserProfile profile) {
        if (rule.getAudienceType() == AudienceType.ALL) return true;
        String profileValue = switch (rule.getAudienceType()) {
            case ROLE -> profile.role();
            case DEPARTMENT -> profile.department();
            case FACULTY -> profile.faculty();
            case SERVICE_UNIT -> profile.serviceUnit();
            case ALL -> null;
        };
        return profileValue != null && profileValue.equalsIgnoreCase(rule.getRuleValue());
    }

    private String normalizeRuleValue(AudienceType type, String value) {
        if (type == AudienceType.ALL) {
            if (value != null && !value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ALL_AUDIENCE_MUST_NOT_HAVE_RULE_VALUE");
            }
            return null;
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TARGETED_AUDIENCE_REQUIRES_RULE_VALUE");
        }
        return value.trim();
    }

    private Announcement find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ANNOUNCEMENT_NOT_FOUND"));
    }

    private void verifyOwner(Announcement announcement, String actorId) {
        if (actorId == null || !announcement.getCreatedBy().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ANNOUNCEMENT_FORBIDDEN");
        }
    }
}
