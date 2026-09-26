package com.group8.communication.feedback;

import java.util.UUID;

/** Local Sprint 2 fallback. The active Spring bean is HttpFeedbackEligibility. */
public class PermissiveFeedbackEligibility implements FeedbackEligibility {
    @Override
    public boolean canSubmit(UUID respondentId, FeedbackForm form) {
        return true;
    }
}
