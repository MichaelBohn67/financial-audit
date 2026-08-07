package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.reporting.ReportContent;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportExportService;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportService;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.ReportRunStatus;
import de.bohnottensen.financialaudit.domain.model.ReportTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportApiController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    // --- Template management ---

    /** Register a new versioned report template. Deactivates the previous active version. */
    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<ReportTemplateView> registerTemplate(
            @RequestBody RegisterTemplateRequest request) {
        ReportTemplate template = reportService.registerTemplate(
                request.name(), request.version(), request.description());
        return ResponseEntity.ok(toTemplateView(template));
    }

    /** List all versions of a named report template, newest first. */
    @GetMapping("/templates/{name}/versions")
    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<List<ReportTemplateView>> templateVersions(@PathVariable String name) {
        List<ReportTemplate> versions = reportService.findTemplateVersions(name);
        return ResponseEntity.ok(versions.stream().map(this::toTemplateView).toList());
    }

    /** Get the currently active template for a given name. */
    @GetMapping("/templates/{name}/active")
    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<ReportTemplateView> activeTemplate(@PathVariable String name) {
        return ResponseEntity.ok(toTemplateView(reportService.findActiveTemplate(name)));
    }

    // --- Report run lifecycle ---

    /**
     * Start a report run against the currently active template.
     * The run is persisted immediately in RUNNING status, then the export artefact
     * is assembled and the run is completed synchronously.
     */
    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<ReportRunView> startRun(
            @RequestBody StartRunRequest request,
            @AuthenticationPrincipal UserDetails user) {
        ReportRun run = reportService.startRun(
                request.templateName(), user.getUsername(), request.parameters());
        try {
            // Assemble and complete synchronously; outputPath records the run ID as reference
            reportExportService.assemble(run.getId());
            run = reportService.completeRun(run.getId(), "run:" + run.getId());
        } catch (Exception e) {
            run = reportService.failRun(run.getId(), e.getMessage());
        }
        return ResponseEntity.ok(toRunView(run));
    }

    /** Get run metadata by ID. */
    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<ReportRunView> getRun(@PathVariable Long id) {
        return ResponseEntity.ok(toRunView(reportService.findRunById(id)));
    }

    /** List runs filtered by status. */
    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<List<ReportRunView>> listRuns(
            @RequestParam(defaultValue = "COMPLETED") String status) {
        List<ReportRun> runs = reportService.findRunsByStatus(ReportRunStatus.valueOf(status));
        return ResponseEntity.ok(runs.stream().map(this::toRunView).toList());
    }

    /**
     * Export the structured report content for a completed run.
     * Returns the assembled {@link ReportContent} as a JSON artefact.
     */
    @GetMapping("/runs/{id}/export")
    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<ReportContent> exportRun(@PathVariable Long id) {
        ReportContent content = reportExportService.assemble(id);
        return ResponseEntity.ok(content);
    }

    // --- View helpers ---

    private ReportTemplateView toTemplateView(ReportTemplate t) {
        return new ReportTemplateView(t.getId(), t.getName(), t.getVersion(),
                t.getDescription(), t.isActive(), t.getCreatedAt());
    }

    private ReportRunView toRunView(ReportRun r) {
        return new ReportRunView(r.getId(), r.getReportName(), r.getTemplateVersion(),
                r.getTemplateId(), r.getStatus(), r.getTriggeredBy(), r.getParameters(),
                r.getOutputPath(), r.getErrorMessage(), r.getGeneratedAt(), r.getCompletedAt());
    }

    // --- Request / Response records ---

    public record RegisterTemplateRequest(String name, String version, String description) {}

    public record StartRunRequest(String templateName, String parameters) {}

    public record ReportTemplateView(Long id, String name, String version, String description,
                                     boolean active, LocalDateTime createdAt) {}

    public record ReportRunView(Long id, String reportName, String templateVersion, Long templateId,
                                String status, String triggeredBy, String parameters,
                                String outputPath, String errorMessage,
                                LocalDateTime generatedAt, LocalDateTime completedAt) {}
}
