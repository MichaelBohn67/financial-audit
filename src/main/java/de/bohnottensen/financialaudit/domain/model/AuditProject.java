package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_projects", uniqueConstraints = @UniqueConstraint(name = "uq_project_tenant_key", columnNames = {"tenant_id", "project_key"}))
public class AuditProject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @Column(name = "project_key", nullable = false, length = 100)
    private String projectKey;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant value) { tenant = value; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String value) { projectKey = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
