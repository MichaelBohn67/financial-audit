package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_key", nullable = false, unique = true, length = 100)
    private String tenantKey;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String value) { tenantKey = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
