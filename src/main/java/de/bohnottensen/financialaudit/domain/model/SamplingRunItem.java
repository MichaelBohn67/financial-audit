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
@Table(name = "sampling_run_items")
public class SamplingRunItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sampling_run_id", nullable = false)
    private Long samplingRunId;

    @Column(name = "sample_unit_index", nullable = false)
    private int sampleUnitIndex;

    @Column(name = "selection_point", nullable = false, precision = 19, scale = 6)
    private BigDecimal selectionPoint;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "booking_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal bookingAmount;

    @Column(name = "cumulative_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal cumulativeAmount;

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

    public Long getSamplingRunId() {
        return samplingRunId;
    }

    public void setSamplingRunId(Long samplingRunId) {
        this.samplingRunId = samplingRunId;
    }

    public int getSampleUnitIndex() {
        return sampleUnitIndex;
    }

    public void setSampleUnitIndex(int sampleUnitIndex) {
        this.sampleUnitIndex = sampleUnitIndex;
    }

    public BigDecimal getSelectionPoint() {
        return selectionPoint;
    }

    public void setSelectionPoint(BigDecimal selectionPoint) {
        this.selectionPoint = selectionPoint;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public BigDecimal getBookingAmount() {
        return bookingAmount;
    }

    public void setBookingAmount(BigDecimal bookingAmount) {
        this.bookingAmount = bookingAmount;
    }

    public BigDecimal getCumulativeAmount() {
        return cumulativeAmount;
    }

    public void setCumulativeAmount(BigDecimal cumulativeAmount) {
        this.cumulativeAmount = cumulativeAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
