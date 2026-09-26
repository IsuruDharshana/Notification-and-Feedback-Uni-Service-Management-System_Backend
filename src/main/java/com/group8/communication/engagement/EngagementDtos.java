package com.group8.communication.engagement;

public final class EngagementDtos {
    private EngagementDtos() {}

    public record SummaryResponse(
            long publishedAnnouncementCount,
            long activeFeedbackFormCount,
            long feedbackResponseCount,
            Double averageFeedbackRating,
            long notificationCount,
            Long eventParticipationCount,
            boolean eventParticipationAvailable) {}
}
