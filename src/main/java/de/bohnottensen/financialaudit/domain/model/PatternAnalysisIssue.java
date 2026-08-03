package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "pattern_analysis_issues")
public class PatternAnalysisIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_run_id", nullable = false)
    private Long patternRunId;

    @Column(name = "issue_type", nullable = false, length = 100)
    private String issueType;

    @Column(nullable = false, length = 50)
    private String severity;

    @Column(name = "reference_key", nullable = false, length = 255)
    private String referenceKey;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(name = "primary_booking_id", nullable = false)
    private Long primaryBookingId;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "details_json", nullable = false, length = 4000)
    private String detailsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getPatternRunId() {
        return patternRunId;
    }

    public void setPatternRunId(Long patternRunId) {
        this.patternRunId = patternRunId;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(String referenceKey) {
        this.referenceKey = referenceKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPrimaryBookingId() {
        return primaryBookingId;
    }

    public void setPrimaryBookingId(Long primaryBookingId) {
        this.primaryBookingId = primaryBookingId;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
