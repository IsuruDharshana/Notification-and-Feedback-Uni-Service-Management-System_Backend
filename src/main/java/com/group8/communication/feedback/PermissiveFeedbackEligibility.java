package com.group8.communication.feedback;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PermissiveFeedbackEligibility implements FeedbackEligibility {
    @Override
    public boolean canSubmit(UUID respondentId, FeedbackForm form) {
        return true;
    }
}
