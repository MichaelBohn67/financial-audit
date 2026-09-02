package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_risks")
public class AuditRisk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "engagement_id", nullable = false) private AuditEngagement engagement;
    @Column(nullable = false, length = 50) private String riskCode;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, length = 4000) private String description;
    @Column(name = "assertion", length = 100) private String assertion;
    @Column(name = "inherent_risk", nullable = false, length = 20) private String inherentRisk;
    @Column(name = "control_risk", nullable = false, length = 20) private String controlRisk;
    @Column(name = "response", length = 4000) private String response;
    @Column(nullable = false, length = 50) private String status = "OPEN";
    @Column(name = "created_by", nullable = false, length = 255) private String createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public AuditEngagement getEngagement() { return engagement; } public void setEngagement(AuditEngagement v) { engagement = v; }
    public String getRiskCode() { return riskCode; } public void setRiskCode(String v) { riskCode = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getAssertion() { return assertion; } public void setAssertion(String v) { assertion = v; }
    public String getInherentRisk() { return inherentRisk; } public void setInherentRisk(String v) { inherentRisk = v; }
    public String getControlRisk() { return controlRisk; } public void setControlRisk(String v) { controlRisk = v; }
    public String getResponse() { return response; } public void setResponse(String v) { response = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
