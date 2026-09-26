package com.group8.communication.engagement;

import com.group8.communication.announcement.AnnouncementRepository;
import com.group8.communication.announcement.AnnouncementStatus;
import com.group8.communication.feedback.FeedbackFormRepository;
import com.group8.communication.feedback.FeedbackResponseRepository;
import com.group8.communication.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private FeedbackFormRepository feedbackFormRepository;
    @Mock private FeedbackResponseRepository feedbackResponseRepository;
    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private EngagementService service;

    @Test
    void returnsOwnedMetricsAndMarksEventParticipationUnavailable() {
        when(announcementRepository.countByStatus(AnnouncementStatus.PUBLISHED)).thenReturn(3L);
        when(feedbackFormRepository.countByActiveTrue()).thenReturn(2L);
        when(feedbackResponseRepository.count()).thenReturn(7L);
        when(feedbackResponseRepository.averageRating()).thenReturn(4.25);
        when(notificationRepository.count()).thenReturn(11L);

        EngagementDtos.SummaryResponse result = service.summary();

        assertEquals(3L, result.publishedAnnouncementCount());
        assertEquals(2L, result.activeFeedbackFormCount());
        assertEquals(7L, result.feedbackResponseCount());
        assertEquals(4.25, result.averageFeedbackRating());
        assertEquals(11L, result.notificationCount());
        assertFalse(result.eventParticipationAvailable());
        assertEquals(null, result.eventParticipationCount());
    }
}
