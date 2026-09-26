package com.group8.communication.announcement;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementStatus status;

    @Column(name = "created_by", nullable = false, columnDefinition = "char(36)")
    private UUID createdBy;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "audience_rule_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private AudienceRule audienceRule;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Announcement() {
    }

    public Announcement(String title, String content, UUID createdBy, AudienceRule audienceRule) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.audienceRule = audienceRule;
        this.status = AnnouncementStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public AnnouncementStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public AudienceRule getAudienceRule() { return audienceRule; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
