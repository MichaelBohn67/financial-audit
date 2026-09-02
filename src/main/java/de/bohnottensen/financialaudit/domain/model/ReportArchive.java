package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_archives", uniqueConstraints = @UniqueConstraint(
        name = "uq_report_archive_run_scope", columnNames = {"report_run_id", "tenant_id", "project_id"}))
public class ReportArchive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "report_run_id", nullable = false) private Long reportRunId;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId;
    @Column(name = "project_id", nullable = false, length = 100) private String projectId;
    @Column(name = "storage_path", nullable = false, length = 1024) private String storagePath;
    @Column(name = "sha256", nullable = false, length = 64) private String sha256;
    @Column(name = "content_length", nullable = false) private Long contentLength;
    @Column(name = "archived_by", nullable = false, length = 255) private String archivedBy;
    @Column(name = "archived_at", nullable = false, updatable = false) private LocalDateTime archivedAt;
    @Column(name = "manifest", nullable = false, length = 4000) private String manifest;

    @PrePersist protected void onCreate() { if (archivedAt == null) archivedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getReportRunId() { return reportRunId; } public void setReportRunId(Long v) { reportRunId = v; }
    public String getTenantId() { return tenantId; } public void setTenantId(String v) { tenantId = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { projectId = v; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String v) { storagePath = v; }
    public String getSha256() { return sha256; } public void setSha256(String v) { sha256 = v; }
    public Long getContentLength() { return contentLength; } public void setContentLength(Long v) { contentLength = v; }
    public String getArchivedBy() { return archivedBy; } public void setArchivedBy(String v) { archivedBy = v; }
    public LocalDateTime getArchivedAt() { return archivedAt; } public void setArchivedAt(LocalDateTime v) { archivedAt = v; }
    public String getManifest() { return manifest; } public void setManifest(String v) { manifest = v; }
}
