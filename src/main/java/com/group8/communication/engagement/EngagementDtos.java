package com.group8.communication.engagement;

import io.swagger.v3.oas.annotations.media.Schema;

public final class EngagementDtos {
    private EngagementDtos() {}

    @Schema(name = "EngagementSummary")
    public record SummaryResponse(
            long publishedAnnouncementCount,
            long activeFeedbackFormCount,
            long feedbackResponseCount,
            @Schema(description = "Null when there are no feedback responses.", nullable = true) Double averageFeedbackRating,
            long notificationCount,
            @Schema(description = "Null while event participation data is unavailable.", nullable = true) Long eventParticipationCount,
            @Schema(description = "False until event participation data is integrated.") boolean eventParticipationAvailable) {}
}
