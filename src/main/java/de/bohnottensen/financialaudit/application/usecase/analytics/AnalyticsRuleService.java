package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.finding.FindingAuditRecorder;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsRuleService {

    private final BookingRepository bookingRepository;
    private final List<AnalyticsRule> rules;
    private final FindingAuditRecorder findingAuditRecorder;

    public AnalyticsRuleService(BookingRepository bookingRepository,
                                FindingAuditRecorder findingAuditRecorder,
                                List<AnalyticsRule> rules) {
        this.bookingRepository = bookingRepository;
        this.findingAuditRecorder = findingAuditRecorder;
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
                finding.setAnalysisRunId("RULE-" + ruleVersion);
                finding.setRuleVersion(ruleVersion);
                finding.setRunContext(runContext);
                findings.add(findingAuditRecorder.record(
                        finding, "SYSTEM_ANALYTICS", "Finding created by analytics rule " + match.ruleName()));
            }
        }
        return findings;
    }
}
