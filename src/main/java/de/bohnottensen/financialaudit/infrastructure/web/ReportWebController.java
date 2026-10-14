package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.reporting.ReportContent;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportExportService;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportService;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reports")
public class ReportWebController {

    private final ReportExportService reportExportService;
    private final ReportService reportService;

    public ReportWebController(ReportExportService reportExportService, ReportService reportService) {
        this.reportExportService = reportExportService;
        this.reportService = reportService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public String viewReport(@PathVariable Long id,
                             @RequestParam String tenantId,
                             @RequestParam String projectId,
                             Model model) {
        ReportRun run = reportService.findRunById(id, tenantId, projectId);
        ReportContent report = reportExportService.assemble(id, tenantId, projectId);
        model.addAttribute("report", report);

        if (run.getReportName() != null && run.getReportName().contains("Management Letter")) {
            return "management-letter";
        }

        return "report-view";
    }
}
