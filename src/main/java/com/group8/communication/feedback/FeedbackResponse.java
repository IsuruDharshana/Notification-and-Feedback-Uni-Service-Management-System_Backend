package com.group8.communication.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback_responses", uniqueConstraints = @UniqueConstraint(
        name = "uk_feedback_response_user_activity",
        columnNames = {"form_id", "respondent_id", "activity_id"}
))
public class FeedbackResponse {
    @Id
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "form_id", nullable = false, columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID formId;

    @Column(name = "activity_id", nullable = false, length = 64)
    private String activityId;

    @Column(name = "respondent_id", nullable = false, length = 64)
    private String respondentId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FeedbackResponse() {
    }

    public FeedbackResponse(UUID formId, String activityId, String respondentId, int rating, String comment) {
        this.id = UUID.randomUUID();
        this.formId = formId;
        this.activityId = activityId;
        this.respondentId = respondentId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getFormId() { return formId; }
    public String getActivityId() { return activityId; }
    public String getRespondentId() { return respondentId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
