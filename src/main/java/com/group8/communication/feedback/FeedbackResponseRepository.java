package com.group8.communication.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FeedbackResponseRepository extends JpaRepository<FeedbackResponse, UUID> {
    boolean existsByFormIdAndActivityIdAndRespondentId(UUID formId, UUID activityId, String respondentId);
    List<FeedbackResponse> findByFormIdOrderByCreatedAtDesc(UUID formId);

    @Query("select avg(response.rating) from FeedbackResponse response")
    Double averageRating();
}
