package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_runs")
public class ReportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "template_version", nullable = false, length = 100)
    private String templateVersion;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "output_path", length = 1024)
    private String outputPath;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
