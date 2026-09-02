package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_engagements")
public class AuditEngagement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId;
    @Column(name = "project_id", nullable = false, length = 100) private String projectId;
    @Column(name = "engagement_key", nullable = false, unique = true, length = 100) private String engagementKey;
    @Column(name = "client_name", nullable = false, length = 255) private String clientName;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(nullable = false, length = 50) private String status = "PLANNING";
    @Column(name = "created_by", nullable = false, length = 255) private String createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String v) { tenantId = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { projectId = v; }
    public String getEngagementKey() { return engagementKey; } public void setEngagementKey(String v) { engagementKey = v; }
    public String getClientName() { return clientName; } public void setClientName(String v) { clientName = v; }
    public LocalDate getPeriodStart() { return periodStart; } public void setPeriodStart(LocalDate v) { periodStart = v; }
    public LocalDate getPeriodEnd() { return periodEnd; } public void setPeriodEnd(LocalDate v) { periodEnd = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
