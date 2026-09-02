package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_procedures")
public class AuditProcedure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "engagement_id", nullable = false) private AuditEngagement engagement;
    @ManyToOne @JoinColumn(name = "risk_id") private AuditRisk risk;
    @ManyToOne @JoinColumn(name = "workpaper_id") private Workpaper workpaper;
    @Column(nullable = false, length = 50) private String procedureCode;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, length = 4000) private String objective;
    @Column(name = "procedure_type", nullable = false, length = 50) private String procedureType;
    @Column(nullable = false, length = 50) private String status = "PLANNED";
    @Column(name = "performed_by", length = 255) private String performedBy;
    @Column(name = "performed_at") private LocalDateTime performedAt;
    @Column(length = 4000) private String conclusion;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public AuditEngagement getEngagement() { return engagement; } public void setEngagement(AuditEngagement v) { engagement = v; }
    public AuditRisk getRisk() { return risk; } public void setRisk(AuditRisk v) { risk = v; }
    public Workpaper getWorkpaper() { return workpaper; } public void setWorkpaper(Workpaper v) { workpaper = v; }
    public String getProcedureCode() { return procedureCode; } public void setProcedureCode(String v) { procedureCode = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getObjective() { return objective; } public void setObjective(String v) { objective = v; }
    public String getProcedureType() { return procedureType; } public void setProcedureType(String v) { procedureType = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getPerformedBy() { return performedBy; } public void setPerformedBy(String v) { performedBy = v; }
    public LocalDateTime getPerformedAt() { return performedAt; } public void setPerformedAt(LocalDateTime v) { performedAt = v; }
    public String getConclusion() { return conclusion; } public void setConclusion(String v) { conclusion = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
