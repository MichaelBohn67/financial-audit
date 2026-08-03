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
@Table(name = "benford_analysis_runs")
public class BenfordAnalysisRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, unique = true, length = 255)
    private String runId;

    @Column(name = "rule_version", nullable = false, length = 255)
    private String ruleVersion;

    @Column(name = "run_context", nullable = false, length = 255)
    private String runContext;

    @Column(name = "booking_count", nullable = false)
    private int bookingCount;

    @Column(name = "suspicious_digit_count", nullable = false)
    private int suspiciousDigitCount;

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

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
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

    public int getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
    }

    public int getSuspiciousDigitCount() {
        return suspiciousDigitCount;
    }

    public void setSuspiciousDigitCount(int suspiciousDigitCount) {
        this.suspiciousDigitCount = suspiciousDigitCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
