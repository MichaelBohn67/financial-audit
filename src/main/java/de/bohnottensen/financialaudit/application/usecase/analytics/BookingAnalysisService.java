package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.AmlEngine;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookingAnalysisService {

    private final BookingRepository bookingRepository;
    private final FindingRepository findingRepository;
    private final AmlEngine amlEngine;
    private final AuditTrailWriter auditTrailWriter;

    public BookingAnalysisService(BookingRepository bookingRepository,
                                  FindingRepository findingRepository,
                                  AmlEngine amlEngine,
                                  AuditTrailWriter auditTrailWriter) {
        this.bookingRepository = bookingRepository;
        this.findingRepository = findingRepository;
        this.amlEngine = amlEngine;
        this.auditTrailWriter = auditTrailWriter;
    }

    public List<Finding> run(AnalysisRunRequest request) {
        String runId = "ANL-" + LocalDateTime.now() + "-" + UUID.randomUUID();
        List<Finding> findings = new ArrayList<>();

        for (Booking booking : bookingRepository.findAll()) {
            List<String> alerts = amlEngine.check(booking);
            for (String alert : alerts) {
                Finding finding = new Finding();
                finding.setBooking(booking);
                finding.setRuleName("AML_RULE");
                finding.setAlertDescription(alert);
                finding.setRiskLevel(resolveSeverity(alert));
                finding.setStatus("NEW");
                finding.setAnalysisRunId(runId);
                finding.setRuleVersion(request.ruleVersion());
                finding.setRunContext(request.runContext());
                Finding savedFinding = findingRepository.save(finding);
                auditTrailWriter.record(
                        "FINDING",
                        savedFinding.getId(),
                        "FINDING_CREATED",
                        "SYSTEM_AML",
                        "Finding created by AML analysis",
                        null,
                        findingSnapshot(savedFinding)
                );
                findings.add(savedFinding);
            }
        }

        return findings;
    }

    private String resolveSeverity(String alert) {
        if (alert != null && alert.toLowerCase().contains("high")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String findingSnapshot(Finding finding) {
        return "bookingId=" + (finding.getBooking() != null ? finding.getBooking().getId() : null)
                + ";ruleName=" + finding.getRuleName()
                + ";riskLevel=" + finding.getRiskLevel()
                + ";status=" + finding.getStatus()
                + ";analysisRunId=" + finding.getAnalysisRunId()
                + ";ruleVersion=" + finding.getRuleVersion()
                + ";runContext=" + finding.getRunContext();
    }
}
