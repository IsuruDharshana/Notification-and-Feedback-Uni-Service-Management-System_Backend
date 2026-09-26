package com.group8.communication.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackFormRepository extends JpaRepository<FeedbackForm, UUID> {
    Optional<FeedbackForm> findByActivityTypeAndActivityId(FeedbackActivityType activityType, UUID activityId);
    List<FeedbackForm> findByActiveTrueOrderByCreatedAtDesc();
}
