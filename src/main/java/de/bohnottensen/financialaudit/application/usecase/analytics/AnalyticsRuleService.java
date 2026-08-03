package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsRuleService {

    private final BookingRepository bookingRepository;
    private final FindingRepository findingRepository;
    private final AuditTrailWriter auditTrailWriter;

    public AnalyticsRuleService(BookingRepository bookingRepository,
                                FindingRepository findingRepository,
                                AuditTrailWriter auditTrailWriter) {
        this.bookingRepository = bookingRepository;
        this.findingRepository = findingRepository;
        this.auditTrailWriter = auditTrailWriter;
    }

    public List<Finding> run(String ruleVersion, String runContext) {
        List<Finding> findings = new ArrayList<>();
        for (Booking booking : bookingRepository.findAll()) {
            if (booking.getAmount() != null && booking.getAmount().compareTo(java.math.BigDecimal.valueOf(100000)) > 0) {
                Finding finding = new Finding();
                finding.setBooking(booking);
                finding.setRuleName("HIGH_AMOUNT_THRESHOLD");
                finding.setAlertDescription("Booking exceeds threshold");
                finding.setRiskLevel("HIGH");
                finding.setStatus("NEW");
                finding.setAnalysisRunId("RULE-" + ruleVersion);
                finding.setRuleVersion(ruleVersion);
                finding.setRunContext(runContext);
                Finding savedFinding = findingRepository.save(finding);
                auditTrailWriter.record(
                        "FINDING",
                        savedFinding.getId(),
                        "FINDING_CREATED",
                        "SYSTEM_ANALYTICS",
                        "Finding created by analytics rule HIGH_AMOUNT_THRESHOLD",
                        null,
                        findingSnapshot(savedFinding)
                );
                findings.add(savedFinding);
            }

            if (booking.getTransactionTimestamp() != null && booking.getTransactionTimestamp().getHour() >= 22) {
                Finding finding = new Finding();
                finding.setBooking(booking);
                finding.setRuleName("TIME_WINDOW_RULE");
                finding.setAlertDescription("Booking outside normal working hours");
                finding.setRiskLevel("MEDIUM");
                finding.setStatus("NEW");
                finding.setAnalysisRunId("RULE-" + ruleVersion);
                finding.setRuleVersion(ruleVersion);
                finding.setRunContext(runContext);
                Finding savedFinding = findingRepository.save(finding);
                auditTrailWriter.record(
                        "FINDING",
                        savedFinding.getId(),
                        "FINDING_CREATED",
                        "SYSTEM_ANALYTICS",
                        "Finding created by analytics rule TIME_WINDOW_RULE",
                        null,
                        findingSnapshot(savedFinding)
                );
                findings.add(savedFinding);
            }
        }
        return findings;
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
