package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "findings")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "workpaper_id")
    private Workpaper workpaper;

    @Column(name = "remediation_owner") private String remediationOwner;
    @Column(name = "remediation_due_date") private java.time.LocalDate remediationDueDate;
    @Column(name = "remediation_status", nullable = false) private String remediationStatus = "OPEN";
    @Column(name = "remediation_plan", length = 4000) private String remediationPlan;
    @Column(name = "resolution_comment", length = 4000) private String resolutionComment;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "resolved_by") private String resolvedBy;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String alertDescription;

    @Column(nullable = false)
    private String riskLevel; // LOW, MEDIUM, HIGH

    @Column(nullable = false)
    private String status; // NEW, EVALUATED, ESCALATED, CLOSED

    @Column(nullable = false)
    private String analysisRunId;

    @Column(nullable = false)
    private String ruleVersion;

    @Column(nullable = false)
    private String runContext;

    private String auditorComment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "NEW";
        if (analysisRunId == null || analysisRunId.isBlank()) analysisRunId = "DEFAULT";
        if (ruleVersion == null || ruleVersion.isBlank()) ruleVersion = "v0";
        if (runContext == null || runContext.isBlank()) runContext = "default";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Workpaper getWorkpaper() { return workpaper; }
    public void setWorkpaper(Workpaper workpaper) { this.workpaper = workpaper; }
    public String getRemediationOwner() { return remediationOwner; }
    public void setRemediationOwner(String value) { remediationOwner = value; }
    public java.time.LocalDate getRemediationDueDate() { return remediationDueDate; }
    public void setRemediationDueDate(java.time.LocalDate value) { remediationDueDate = value; }
    public String getRemediationStatus() { return remediationStatus; }
    public void setRemediationStatus(String value) { remediationStatus = value; }
    public String getRemediationPlan() { return remediationPlan; }
    public void setRemediationPlan(String value) { remediationPlan = value; }
    public String getResolutionComment() { return resolutionComment; }
    public void setResolutionComment(String value) { resolutionComment = value; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime value) { resolvedAt = value; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String value) { resolvedBy = value; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getAlertDescription() { return alertDescription; }
    public void setAlertDescription(String alertDescription) { this.alertDescription = alertDescription; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAuditorComment() { return auditorComment; }
    public void setAuditorComment(String auditorComment) { this.auditorComment = auditorComment; }

    public String getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(String analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getRunContext() {
        return runContext;
    }

    public void setRunContext(String runContext) {
        this.runContext = runContext;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
