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
@Table(name = "materiality_configs")
public class MaterialityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "planning_materiality")
    private BigDecimal planningMateriality;

    @Column(name = "performance_materiality")
    private BigDecimal performanceMateriality;

    @Column(name = "de_minimis_threshold", nullable = false)
    private BigDecimal deMinimisThreshold;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "tolerable_error_rate")
    private BigDecimal tolerableErrorRate;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPlanningMateriality() {
        return planningMateriality;
    }

    public void setPlanningMateriality(BigDecimal planningMateriality) {
        this.planningMateriality = planningMateriality;
    }

    public BigDecimal getPerformanceMateriality() {
        return performanceMateriality;
    }

    public void setPerformanceMateriality(BigDecimal performanceMateriality) {
        this.performanceMateriality = performanceMateriality;
    }

    public BigDecimal getDeMinimisThreshold() { return deMinimisThreshold; }
    public void setDeMinimisThreshold(BigDecimal value) { this.deMinimisThreshold = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getTolerableErrorRate() {
        return tolerableErrorRate;
    }

    public void setTolerableErrorRate(BigDecimal tolerableErrorRate) {
        this.tolerableErrorRate = tolerableErrorRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
