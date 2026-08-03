package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class AuditEventMetadata {

    @Column(nullable = false, length = 255)
    private String actor;

    @Column(nullable = false, length = 1024)
    private String summary;

    @Column(name = "previous_value", length = 4000)
    private String previousValue;

    @Column(name = "current_value", length = 4000)
    private String currentValue;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public void initializeOccurredAtIfMissing() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
