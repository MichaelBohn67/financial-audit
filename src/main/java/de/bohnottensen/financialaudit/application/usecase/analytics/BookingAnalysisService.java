package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.application.usecase.finding.FindingAuditRecorder;
import de.bohnottensen.financialaudit.domain.model.AmlEngine;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookingAnalysisService {

    private final BookingRepository bookingRepository;
    private final AmlEngine amlEngine;
    private final FindingAuditRecorder findingAuditRecorder;

    public BookingAnalysisService(BookingRepository bookingRepository,
                                  AmlEngine amlEngine,
                                  FindingAuditRecorder findingAuditRecorder) {
        this.bookingRepository = bookingRepository;
        this.amlEngine = amlEngine;
        this.findingAuditRecorder = findingAuditRecorder;
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
                finding.setAnalysisRunId(runId);
                finding.setRuleVersion(request.ruleVersion());
                finding.setRunContext(request.runContext());
                findings.add(findingAuditRecorder.record(finding, "SYSTEM_AML", "Finding created by AML analysis"));
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
}
