package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.PatternAnalysisIssue;
import de.bohnottensen.financialaudit.domain.model.PatternAnalysisRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.PatternAnalysisIssueRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.PatternAnalysisRunRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PatternAnalysisService {

    private final BookingRepository bookingRepository;
    private final FindingRepository findingRepository;
    private final PatternAnalysisRunRepository patternAnalysisRunRepository;
    private final PatternAnalysisIssueRepository patternAnalysisIssueRepository;
    private final AuditTrailWriter auditTrailWriter;

    public PatternAnalysisService(BookingRepository bookingRepository,
                                  FindingRepository findingRepository,
                                  PatternAnalysisRunRepository patternAnalysisRunRepository,
                                  PatternAnalysisIssueRepository patternAnalysisIssueRepository,
                                  AuditTrailWriter auditTrailWriter) {
        this.bookingRepository = bookingRepository;
        this.findingRepository = findingRepository;
        this.patternAnalysisRunRepository = patternAnalysisRunRepository;
        this.patternAnalysisIssueRepository = patternAnalysisIssueRepository;
        this.auditTrailWriter = auditTrailWriter;
    }

    public PatternAnalysisResult run(String ruleVersion, String runContext) {
        List<Booking> bookings = bookingRepository.findAll();
        String runId = "PATTERN-" + LocalDateTime.now() + "-" + UUID.randomUUID();

        PatternAnalysisRun run = new PatternAnalysisRun();
        run.setRunId(runId);
        run.setRuleVersion(ruleVersion);
        run.setRunContext(runContext);
        run.setBookingCount(bookings.size());
        run.setIssueCount(0);
        run = patternAnalysisRunRepository.save(run);

        List<DetectedIssue> detectedIssues = new ArrayList<>();
        detectDuplicateDocumentNumbers(bookings, detectedIssues);
        detectDocumentNumberGaps(bookings, detectedIssues);
        detectRepeatedTransferPatterns(bookings, detectedIssues);

        List<PatternAnalysisResult.IssueResult> resultIssues = new ArrayList<>();
        for (DetectedIssue detectedIssue : detectedIssues) {
            PatternAnalysisIssue issue = new PatternAnalysisIssue();
            issue.setPatternRunId(run.getId());
            issue.setIssueType(detectedIssue.issueType());
            issue.setSeverity(detectedIssue.severity());
            issue.setReferenceKey(detectedIssue.referenceKey());
            issue.setDescription(detectedIssue.description());
            issue.setPrimaryBookingId(detectedIssue.primaryBooking().getId());
            issue.setOccurrenceCount(detectedIssue.occurrenceCount());
            issue.setDetailsJson(detectedIssue.detailsJson());
            patternAnalysisIssueRepository.save(issue);

            persistFinding(runId, ruleVersion, runContext, detectedIssue);

            resultIssues.add(new PatternAnalysisResult.IssueResult(
                    detectedIssue.issueType(),
                    detectedIssue.severity(),
                    detectedIssue.referenceKey(),
                    detectedIssue.description(),
                    detectedIssue.primaryBooking().getId(),
                    detectedIssue.occurrenceCount(),
                    detectedIssue.detailsJson()
            ));
        }

        run.setIssueCount(resultIssues.size());
        run = patternAnalysisRunRepository.save(run);

        return new PatternAnalysisResult(
                run.getId(),
                run.getRunId(),
                run.getRuleVersion(),
                run.getRunContext(),
                run.getBookingCount(),
                run.getIssueCount(),
                run.getCreatedAt(),
                List.copyOf(resultIssues)
        );
    }

    private void detectDuplicateDocumentNumbers(List<Booking> bookings, List<DetectedIssue> issues) {
        Map<Long, List<Booking>> grouped = new HashMap<>();
        for (Booking booking : bookings) {
            if (booking.getForeignTransactionId() != null) {
                grouped.computeIfAbsent(booking.getForeignTransactionId(), ignored -> new ArrayList<>()).add(booking);
            }
        }
        grouped.forEach((foreignId, duplicateBookings) -> {
            if (duplicateBookings.size() > 1) {
                Booking primary = duplicateBookings.get(0);
                issues.add(new DetectedIssue(
                        "DUPLICATE_DOCUMENT_NUMBER",
                        "HIGH",
                        "foreignTransactionId=" + foreignId,
                        "Duplicate document number detected for foreign transaction id " + foreignId,
                        primary,
                        duplicateBookings.size(),
                        "{\"foreignTransactionId\":\"" + foreignId + "\",\"bookingIds\":\"" + bookingIds(duplicateBookings) + "\"}"
                ));
            }
        });
    }

    private void detectDocumentNumberGaps(List<Booking> bookings, List<DetectedIssue> issues) {
        List<Booking> withIds = bookings.stream()
                .filter(booking -> booking.getForeignTransactionId() != null)
                .sorted(Comparator.comparing(Booking::getForeignTransactionId))
                .toList();
        for (int i = 1; i < withIds.size(); i++) {
            long previous = withIds.get(i - 1).getForeignTransactionId();
            long current = withIds.get(i).getForeignTransactionId();
            if (current - previous > 1) {
                Booking primary = withIds.get(i);
                issues.add(new DetectedIssue(
                        "DOCUMENT_NUMBER_GAP",
                        "MEDIUM",
                        "gap=" + previous + "-" + current,
                        "Gap in document numbers between " + previous + " and " + current,
                        primary,
                        (int) (current - previous - 1),
                        "{\"previous\":\"" + previous + "\",\"current\":\"" + current + "\",\"missingCount\":\"" + (current - previous - 1) + "\"}"
                ));
            }
        }
    }

    private void detectRepeatedTransferPatterns(List<Booking> bookings, List<DetectedIssue> issues) {
        Map<String, List<Booking>> grouped = new HashMap<>();
        for (Booking booking : bookings) {
            if (booking.getAmount() == null
                    || booking.getCurrency() == null
                    || booking.getSourceAccount() == null
                    || booking.getDestinationAccount() == null
                    || booking.getTransactionTimestamp() == null) {
                continue;
            }
            String key = patternKey(booking);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(booking);
        }

        grouped.forEach((key, groupedBookings) -> {
            if (groupedBookings.size() < 3) {
                return;
            }
            List<Booking> sorted = groupedBookings.stream()
                    .sorted(Comparator.comparing(Booking::getTransactionTimestamp))
                    .toList();
            for (int i = 2; i < sorted.size(); i++) {
                Booking first = sorted.get(i - 2);
                Booking third = sorted.get(i);
                if (Duration.between(first.getTransactionTimestamp(), third.getTransactionTimestamp()).toHours() <= 24) {
                    Booking primary = sorted.get(i - 1);
                    issues.add(new DetectedIssue(
                            "REPEATED_TRANSFER_PATTERN",
                            "MEDIUM",
                            key,
                            "Repeated transfer pattern detected within 24 hours",
                            primary,
                            sorted.size(),
                            "{\"patternKey\":\"" + key + "\",\"bookingIds\":\"" + bookingIds(sorted) + "\"}"
                    ));
                    break;
                }
            }
        });
    }

    private void persistFinding(String runId, String ruleVersion, String runContext, DetectedIssue issue) {
        Finding finding = new Finding();
        finding.setBooking(issue.primaryBooking());
        finding.setRuleName("PATTERN_" + issue.issueType());
        finding.setAlertDescription(issue.description());
        finding.setRiskLevel(issue.severity());
        finding.setStatus("NEW");
        finding.setAnalysisRunId(runId);
        finding.setRuleVersion(ruleVersion);
        finding.setRunContext(runContext);
        Finding savedFinding = findingRepository.save(finding);
        auditTrailWriter.record(
                "FINDING",
                savedFinding.getId(),
                "FINDING_CREATED",
                "SYSTEM_PATTERN_ANALYSIS",
                "Finding created by pattern analysis",
                null,
                "bookingId=" + (savedFinding.getBooking() != null ? savedFinding.getBooking().getId() : null)
                        + ";ruleName=" + savedFinding.getRuleName()
                        + ";riskLevel=" + savedFinding.getRiskLevel()
                        + ";status=" + savedFinding.getStatus()
                        + ";analysisRunId=" + savedFinding.getAnalysisRunId()
                        + ";ruleVersion=" + savedFinding.getRuleVersion()
                        + ";runContext=" + savedFinding.getRunContext()
        );
    }

    private String patternKey(Booking booking) {
        BigDecimal normalizedAmount = booking.getAmount().stripTrailingZeros();
        return booking.getSourceAccount() + "|" + booking.getDestinationAccount() + "|"
                + booking.getCurrency() + "|" + normalizedAmount.toPlainString();
    }

    private String bookingIds(List<Booking> bookings) {
        return bookings.stream()
                .map(booking -> String.valueOf(booking.getId()))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private record DetectedIssue(
            String issueType,
            String severity,
            String referenceKey,
            String description,
            Booking primaryBooking,
            int occurrenceCount,
            String detailsJson
    ) {
    }
}
