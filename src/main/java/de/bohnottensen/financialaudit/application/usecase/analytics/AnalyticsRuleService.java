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
    private final List<AnalyticsRule> rules;

    public AnalyticsRuleService(BookingRepository bookingRepository,
                                FindingRepository findingRepository,
                                AuditTrailWriter auditTrailWriter,
                                List<AnalyticsRule> rules) {
        this.bookingRepository = bookingRepository;
        this.findingRepository = findingRepository;
        this.auditTrailWriter = auditTrailWriter;
        this.rules = rules;
    }

    public List<Finding> run(String ruleVersion, String runContext) {
        List<Finding> findings = new ArrayList<>();
        List<Booking> bookings = bookingRepository.findAll();
        for (AnalyticsRule rule : rules) {
            List<AnalyticsRuleMatch> matches = rule.evaluate(bookings);
            for (AnalyticsRuleMatch match : matches) {
                Finding finding = new Finding();
                finding.setBooking(match.booking());
                finding.setRuleName(match.ruleName());
                finding.setAlertDescription(match.alertDescription());
                finding.setRiskLevel(match.riskLevel());
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
                        "Finding created by analytics rule " + match.ruleName(),
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
