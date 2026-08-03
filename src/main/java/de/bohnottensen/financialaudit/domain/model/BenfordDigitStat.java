package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "benford_digit_stats")
public class BenfordDigitStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "benford_run_id", nullable = false)
    private Long benfordRunId;

    @Column(name = "leading_digit", nullable = false)
    private int leadingDigit;

    @Column(name = "expected_ratio", nullable = false, precision = 10, scale = 6)
    private BigDecimal expectedRatio;

    @Column(name = "observed_ratio", nullable = false, precision = 10, scale = 6)
    private BigDecimal observedRatio;

    @Column(name = "absolute_deviation", nullable = false, precision = 10, scale = 6)
    private BigDecimal absoluteDeviation;

    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

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

    public Long getBenfordRunId() {
        return benfordRunId;
    }

    public void setBenfordRunId(Long benfordRunId) {
        this.benfordRunId = benfordRunId;
    }

    public int getLeadingDigit() {
        return leadingDigit;
    }

    public void setLeadingDigit(int leadingDigit) {
        this.leadingDigit = leadingDigit;
    }

    public BigDecimal getExpectedRatio() {
        return expectedRatio;
    }

    public void setExpectedRatio(BigDecimal expectedRatio) {
        this.expectedRatio = expectedRatio;
    }

    public BigDecimal getObservedRatio() {
        return observedRatio;
    }

    public void setObservedRatio(BigDecimal observedRatio) {
        this.observedRatio = observedRatio;
    }

    public BigDecimal getAbsoluteDeviation() {
        return absoluteDeviation;
    }

    public void setAbsoluteDeviation(BigDecimal absoluteDeviation) {
        this.absoluteDeviation = absoluteDeviation;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
