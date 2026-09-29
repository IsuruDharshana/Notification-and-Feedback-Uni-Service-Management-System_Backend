package com.group8.communication.engagement;

import com.group8.communication.announcement.AnnouncementRepository;
import com.group8.communication.announcement.AnnouncementStatus;
import com.group8.communication.feedback.FeedbackFormRepository;
import com.group8.communication.feedback.FeedbackResponseRepository;
import com.group8.communication.notification.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class EngagementService {
    private final AnnouncementRepository announcementRepository;
    private final FeedbackFormRepository feedbackFormRepository;
    private final FeedbackResponseRepository feedbackResponseRepository;
    private final NotificationRepository notificationRepository;

    public EngagementService(AnnouncementRepository announcementRepository,
                             FeedbackFormRepository feedbackFormRepository,
                             FeedbackResponseRepository feedbackResponseRepository,
                             NotificationRepository notificationRepository) {
        this.announcementRepository = announcementRepository;
        this.feedbackFormRepository = feedbackFormRepository;
        this.feedbackResponseRepository = feedbackResponseRepository;
        this.notificationRepository = notificationRepository;
    }

    public EngagementDtos.SummaryResponse summary() {
        return new EngagementDtos.SummaryResponse(
                announcementRepository.countByStatus(AnnouncementStatus.PUBLISHED),
                feedbackFormRepository.countByActiveTrue(),
                feedbackResponseRepository.count(),
                feedbackResponseRepository.averageRating(),
                notificationRepository.count(),
                null,
                false);
    }
}
