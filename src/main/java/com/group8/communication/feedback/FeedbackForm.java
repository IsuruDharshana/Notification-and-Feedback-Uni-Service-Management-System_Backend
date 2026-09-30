package com.group8.communication.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback_forms", uniqueConstraints = @UniqueConstraint(
        name = "uk_feedback_forms_activity",
        columnNames = {"activity_type", "activity_id"}
))
public class FeedbackForm {
    @Id
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private FeedbackActivityType activityType;

    @Column(name = "activity_id", nullable = false, columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID activityId;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "questions_json", nullable = false, columnDefinition = "text")
    private String questionsJson;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected FeedbackForm() {
    }

    public FeedbackForm(FeedbackActivityType activityType, UUID activityId, String title, String questionsJson, String createdBy) {
        this.id = UUID.randomUUID();
        this.activityType = activityType;
        this.activityId = activityId;
        this.createdBy = createdBy;
        this.title = title;
        this.questionsJson = questionsJson;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public FeedbackActivityType getActivityType() { return activityType; }
    public UUID getActivityId() { return activityId; }
    public String getCreatedBy() { return createdBy; }
    public String getTitle() { return title; }
    public String getQuestionsJson() { return questionsJson; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
