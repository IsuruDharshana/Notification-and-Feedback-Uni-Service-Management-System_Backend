package com.group8.communication.announcement;

import com.group8.communication.integration.UserDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {
    @Mock private AnnouncementRepository repository;
    @Mock private UserDirectory userDirectory;
    @InjectMocks private AnnouncementService service;

    @Test
    void createsAllAudienceDraft() {
        UUID creator = UUID.randomUUID();
        when(repository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = service.create(new AnnouncementDtos.CreateRequest(
                "Library hours", "The library is open longer this week.", AudienceType.ALL, null), creator);

        assertEquals(AnnouncementStatus.DRAFT, result.getStatus());
        assertEquals(AudienceType.ALL, result.getAudienceRule().getAudienceType());
        verify(repository).save(any(Announcement.class));
    }

    @Test
    void rejectsTargetedAnnouncementWithoutRuleValue() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.create(
                new AnnouncementDtos.CreateRequest("Notice", "Content", AudienceType.ROLE, "  "), UUID.randomUUID()));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test
    void visibleReturnsAllAndMatchingTargetOnly() {
        Announcement all = new Announcement("All", "Everyone", UUID.randomUUID(), new AudienceRule(AudienceType.ALL, null));
        Announcement student = new Announcement("Students", "Students", UUID.randomUUID(), new AudienceRule(AudienceType.ROLE, "STUDENT"));
        Announcement staff = new Announcement("Staff", "Staff", UUID.randomUUID(), new AudienceRule(AudienceType.ROLE, "STAFF"));
        all.publish(java.time.LocalDateTime.now());
        student.publish(java.time.LocalDateTime.now());
        staff.publish(java.time.LocalDateTime.now());
        when(repository.findByStatusOrderByPublishedAtDesc(AnnouncementStatus.PUBLISHED))
                .thenReturn(List.of(all, student, staff));

        List<AnnouncementDtos.Response> result = service.visible(AudienceType.ROLE, "student");

        assertEquals(2, result.size());
        assertEquals("All", result.get(0).title());
        assertEquals("Students", result.get(1).title());
    }
}
