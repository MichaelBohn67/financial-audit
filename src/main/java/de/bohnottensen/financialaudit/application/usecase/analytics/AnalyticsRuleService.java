package de.bohnottensen.financialaudit.application.usecase.analytics;

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

    public AnalyticsRuleService(BookingRepository bookingRepository, FindingRepository findingRepository) {
        this.bookingRepository = bookingRepository;
        this.findingRepository = findingRepository;
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
                findings.add(findingRepository.save(finding));
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
                findings.add(findingRepository.save(finding));
            }
        }
        return findings;
    }
}
