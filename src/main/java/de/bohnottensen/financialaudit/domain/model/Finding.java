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
