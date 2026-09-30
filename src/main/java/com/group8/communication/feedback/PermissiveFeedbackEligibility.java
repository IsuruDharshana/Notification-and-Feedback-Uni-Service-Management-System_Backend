package com.group8.communication.feedback;

/** Local Sprint 2 fallback. The active Spring bean is HttpFeedbackEligibility. */
public class PermissiveFeedbackEligibility implements FeedbackEligibility {
    @Override
    public boolean canSubmit(String respondentId, FeedbackForm form) {
        return true;
    }
}
