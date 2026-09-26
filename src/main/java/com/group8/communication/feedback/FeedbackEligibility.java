package com.group8.communication.feedback;

import java.util.UUID;

/**
 * Integration seam for the Group 7 completion-status contract.
 * Sprint 3 can replace the permissive implementation with the real client.
 */
public interface FeedbackEligibility {
    boolean canSubmit(UUID respondentId, FeedbackForm form);
}
