package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.BenfordAnalysisRun;
import de.bohnottensen.financialaudit.domain.model.BenfordDigitStat;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BenfordAnalysisRunRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BenfordDigitStatRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BenfordAnalysisService {

    private static final BigDecimal SUSPICIOUS_DEVIATION_THRESHOLD = new BigDecimal("0.08");

    private final BookingRepository bookingRepository;
    private final FindingRepository findingRepository;
    private final BenfordAnalysisRunRepository benfordAnalysisRunRepository;
    private final BenfordDigitStatRepository benfordDigitStatRepository;
    private final AuditTrailWriter auditTrailWriter;

    public BenfordAnalysisService(BookingRepository bookingRepository,
                                  FindingRepository findingRepository,
                                  BenfordAnalysisRunRepository benfordAnalysisRunRepository,
                                  BenfordDigitStatRepository benfordDigitStatRepository,
                                  AuditTrailWriter auditTrailWriter) {
        this.bookingRepository = bookingRepository;
        this.findingRepository = findingRepository;
        this.benfordAnalysisRunRepository = benfordAnalysisRunRepository;
        this.benfordDigitStatRepository = benfordDigitStatRepository;
        this.auditTrailWriter = auditTrailWriter;
    }

    public BenfordAnalysisResult run(String ruleVersion, String runContext) {
        List<Booking> bookings = bookingRepository.findAll();
        List<Booking> eligibleBookings = bookings.stream()
                .filter(this::isEligibleForBenford)
                .toList();
        int sampleSize = eligibleBookings.size();
        int[] observedCounts = observedLeadingDigitCounts(eligibleBookings);

        String runId = "BENFORD-" + LocalDateTime.now() + "-" + UUID.randomUUID();
        BenfordAnalysisRun run = new BenfordAnalysisRun();
        run.setRunId(runId);
        run.setRuleVersion(ruleVersion);
        run.setRunContext(runContext);
        run.setBookingCount(sampleSize);
        run.setSuspiciousDigitCount(0);
        run = benfordAnalysisRunRepository.save(run);

        List<BenfordAnalysisResult.DigitResult> digitResults = new ArrayList<>();
        int suspiciousDigitCount = 0;

        for (int digit = 1; digit <= 9; digit++) {
            BigDecimal expectedRatio = benfordExpectedRatio(digit);
            BigDecimal observedRatio = sampleSize == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(observedCounts[digit]).divide(BigDecimal.valueOf(sampleSize), 6, RoundingMode.HALF_UP);
            BigDecimal absoluteDeviation = observedRatio.subtract(expectedRatio).abs().setScale(6, RoundingMode.HALF_UP);

            BenfordDigitStat stat = new BenfordDigitStat();
            stat.setBenfordRunId(run.getId());
            stat.setLeadingDigit(digit);
            stat.setExpectedRatio(expectedRatio);
            stat.setObservedRatio(observedRatio);
            stat.setAbsoluteDeviation(absoluteDeviation);
            stat.setSampleSize(sampleSize);
            benfordDigitStatRepository.save(stat);

            digitResults.add(new BenfordAnalysisResult.DigitResult(
                    digit,
                    expectedRatio,
                    observedRatio,
                    absoluteDeviation,
                    sampleSize
            ));

            if (absoluteDeviation.compareTo(SUSPICIOUS_DEVIATION_THRESHOLD) >= 0 && sampleSize > 0) {
                suspiciousDigitCount++;
                persistBenfordFinding(runId, ruleVersion, runContext, digit, observedRatio, expectedRatio, absoluteDeviation, eligibleBookings.get(0));
            }
        }

        run.setSuspiciousDigitCount(suspiciousDigitCount);
        run = benfordAnalysisRunRepository.save(run);

        return new BenfordAnalysisResult(
                run.getId(),
                run.getRunId(),
                run.getRuleVersion(),
                run.getRunContext(),
                run.getBookingCount(),
                run.getSuspiciousDigitCount(),
                run.getCreatedAt(),
                List.copyOf(digitResults)
        );
    }

    private boolean isEligibleForBenford(Booking booking) {
        return booking.getAmount() != null
                && booking.getAmount().compareTo(BigDecimal.ZERO) > 0
                && leadingDigit(booking) >= 1;
    }

    private int[] observedLeadingDigitCounts(List<Booking> bookings) {
        int[] counts = new int[10];
        for (Booking booking : bookings) {
            int digit = leadingDigit(booking);
            if (digit >= 1 && digit <= 9) {
                counts[digit]++;
            }
        }
        return counts;
    }

    private int leadingDigit(Booking booking) {
        String normalized = booking.getAmount().abs().stripTrailingZeros().toPlainString().replace(".", "");
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= '1' && c <= '9') {
                return c - '0';
            }
        }
        return -1;
    }

    private BigDecimal benfordExpectedRatio(int digit) {
        double value = Math.log10(1d + (1d / digit));
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private void persistBenfordFinding(String runId,
                                       String ruleVersion,
                                       String runContext,
                                       int digit,
                                       BigDecimal observedRatio,
                                       BigDecimal expectedRatio,
                                       BigDecimal deviation,
                                       Booking representativeBooking) {
        Finding finding = new Finding();
        finding.setBooking(representativeBooking);
        finding.setRuleName("BENFORD_DIGIT_" + digit);
        finding.setAlertDescription("Benford deviation for leading digit " + digit
                + " observed=" + observedRatio
                + " expected=" + expectedRatio
                + " deviation=" + deviation);
        finding.setRiskLevel("MEDIUM");
        finding.setStatus("NEW");
        finding.setAnalysisRunId(runId);
        finding.setRuleVersion(ruleVersion);
        finding.setRunContext(runContext);
        Finding savedFinding = findingRepository.save(finding);
        auditTrailWriter.record(
                "FINDING",
                savedFinding.getId(),
                "FINDING_CREATED",
                "SYSTEM_BENFORD",
                "Finding created by Benford analysis",
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
}
