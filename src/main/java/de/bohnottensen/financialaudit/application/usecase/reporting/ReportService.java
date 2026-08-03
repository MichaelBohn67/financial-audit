package de.bohnottensen.financialaudit.application.usecase.reporting;

import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.ReportRunStatus;
import de.bohnottensen.financialaudit.domain.model.ReportTemplate;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportTemplateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final ReportRunRepository reportRunRepository;
    private final ReportTemplateRepository reportTemplateRepository;

    public ReportService(ReportRunRepository reportRunRepository,
                         ReportTemplateRepository reportTemplateRepository) {
        this.reportRunRepository = reportRunRepository;
        this.reportTemplateRepository = reportTemplateRepository;
    }

    // --- Template management ---

    /**
     * Register a new versioned report template. If another template with the same name
     * exists and is active, it is deactivated so this version becomes the active one.
     */
    public ReportTemplate registerTemplate(String name, String version, String description) {
        reportTemplateRepository.findByNameAndVersion(name, version).ifPresent(existing -> {
            throw new IllegalArgumentException("Template already exists: " + name + " v" + version);
        });
        reportTemplateRepository.findFirstByNameAndActiveTrueOrderByCreatedAtDesc(name)
                .ifPresent(previous -> {
                    previous.setActive(false);
                    reportTemplateRepository.save(previous);
                });
        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setVersion(version);
        template.setDescription(description);
        template.setActive(true);
        return reportTemplateRepository.save(template);
    }

    public ReportTemplate findActiveTemplate(String name) {
        return reportTemplateRepository
                .findFirstByNameAndActiveTrueOrderByCreatedAtDesc(name)
                .orElseThrow(() -> new IllegalArgumentException("No active template found: " + name));
    }

    public List<ReportTemplate> findTemplateVersions(String name) {
        return reportTemplateRepository.findByNameOrderByCreatedAtDesc(name);
    }

    // --- Run lifecycle ---

    /**
     * Start a report run for the currently active template with the given name.
     * The run is created in RUNNING status and persisted immediately for traceability.
     */
    public ReportRun startRun(String templateName, String triggeredBy, String parameters) {
        ReportTemplate template = findActiveTemplate(templateName);
        ReportRun run = new ReportRun();
        run.setReportName(templateName);
        run.setTemplateVersion(template.getVersion());
        run.setTemplateId(template.getId());
        run.setTriggeredBy(triggeredBy);
        run.setParameters(parameters);
        run.setStatus(ReportRunStatus.RUNNING.name());
        return reportRunRepository.save(run);
    }

    /**
     * Mark a run as completed with an output path.
     */
    public ReportRun completeRun(Long runId, String outputPath) {
        ReportRun run = reportRunRepository.findById(runId).orElseThrow();
        run.setStatus(ReportRunStatus.COMPLETED.name());
        run.setOutputPath(outputPath);
        run.setCompletedAt(LocalDateTime.now());
        return reportRunRepository.save(run);
    }

    /**
     * Mark a run as failed with an error message.
     */
    public ReportRun failRun(Long runId, String errorMessage) {
        ReportRun run = reportRunRepository.findById(runId).orElseThrow();
        run.setStatus(ReportRunStatus.FAILED.name());
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(LocalDateTime.now());
        return reportRunRepository.save(run);
    }

    public ReportRun findRunById(Long runId) {
        return reportRunRepository.findById(runId).orElseThrow();
    }

    public List<ReportRun> findRunsByStatus(ReportRunStatus status) {
        return reportRunRepository.findByStatusOrderByGeneratedAtDesc(status.name());
    }

    public List<ReportRun> findRunsByTemplate(String templateName, String templateVersion) {
        return reportRunRepository.findByReportNameAndTemplateVersionOrderByGeneratedAtDesc(
                templateName, templateVersion);
    }

    /**
     * Legacy method for backward compatibility: directly generate and persist a run result.
     */
    public ReportRun generate(String reportName, String templateVersion, String status, String outputPath) {
        ReportRun reportRun = new ReportRun();
        reportRun.setReportName(reportName);
        reportRun.setTemplateVersion(templateVersion);
        reportRun.setStatus(status);
        reportRun.setOutputPath(outputPath);
        return reportRunRepository.save(reportRun);
    }
}
