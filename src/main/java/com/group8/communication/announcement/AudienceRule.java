package com.group8.communication.announcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audience_rules")
public class AudienceRule {
    @Id
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private AudienceType audienceType;

    @Column(name = "rule_value", length = 100)
    private String ruleValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AudienceRule() {
    }

    public AudienceRule(AudienceType audienceType, String ruleValue) {
        this.id = UUID.randomUUID();
        this.audienceType = audienceType;
        this.ruleValue = ruleValue;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AudienceType getAudienceType() { return audienceType; }
    public String getRuleValue() { return ruleValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
