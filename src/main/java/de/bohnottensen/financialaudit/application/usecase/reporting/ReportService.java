package de.bohnottensen.financialaudit.application.usecase.reporting;

import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportRunRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ReportRunRepository reportRunRepository;

    public ReportService(ReportRunRepository reportRunRepository) {
        this.reportRunRepository = reportRunRepository;
    }

    public ReportRun generate(String reportName, String templateVersion, String status, String outputPath) {
        ReportRun reportRun = new ReportRun();
        reportRun.setReportName(reportName);
        reportRun.setTemplateVersion(templateVersion);
        reportRun.setStatus(status);
        reportRun.setOutputPath(outputPath);
        return reportRunRepository.save(reportRun);
    }
}
