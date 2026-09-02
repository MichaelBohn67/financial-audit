package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_memberships", uniqueConstraints = @UniqueConstraint(name = "uq_project_membership", columnNames = {"username", "project_id"}))
public class ProjectMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255) private String username;
    @ManyToOne(optional = false) @JoinColumn(name = "project_id", nullable = false) private AuditProject project;
    @Column(name = "assigned_at", nullable = false, updatable = false) private LocalDateTime assignedAt;
    @PrePersist void onCreate() { if (assignedAt == null) assignedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public AuditProject getProject() { return project; }
    public void setProject(AuditProject value) { project = value; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
