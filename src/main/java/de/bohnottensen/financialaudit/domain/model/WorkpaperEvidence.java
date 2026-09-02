package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workpaper_evidence")
public class WorkpaperEvidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "workpaper_id", nullable = false) private Workpaper workpaper;
    @ManyToOne @JoinColumn(name = "procedure_id") private AuditProcedure procedure;
    @Column(name = "evidence_type", nullable = false, length = 50) private String evidenceType;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "reference_uri", nullable = false, length = 1024) private String referenceUri;
    @Column(name = "sha256", nullable = false, length = 64) private String sha256;
    @Column(name = "mime_type", length = 255) private String mimeType;
    @Column(name = "uploaded_by", nullable = false, length = 255) private String uploadedBy;
    @Column(name = "uploaded_at", nullable = false, updatable = false) private LocalDateTime uploadedAt;
    @PrePersist void onCreate() { if (uploadedAt == null) uploadedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Workpaper getWorkpaper() { return workpaper; } public void setWorkpaper(Workpaper v) { workpaper = v; }
    public AuditProcedure getProcedure() { return procedure; } public void setProcedure(AuditProcedure v) { procedure = v; }
    public String getEvidenceType() { return evidenceType; } public void setEvidenceType(String v) { evidenceType = v; }
    public String getFileName() { return fileName; } public void setFileName(String v) { fileName = v; }
    public String getReferenceUri() { return referenceUri; } public void setReferenceUri(String v) { referenceUri = v; }
    public String getSha256() { return sha256; } public void setSha256(String v) { sha256 = v; }
    public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { uploadedBy = v; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
