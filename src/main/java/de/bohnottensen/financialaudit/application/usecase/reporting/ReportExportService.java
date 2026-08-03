package de.bohnottensen.financialaudit.application.usecase.reporting;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembles structured {@link ReportContent} from live audit artefacts and
 * serializes it as a JSON export artefact.
 * All logic is deterministic: same data always produces the same output.
 */
@Service
public class ReportExportService {

    private final FindingRepository findingRepository;
    private final BookingRepository bookingRepository;
    private final SamplingRunRepository samplingRunRepository;
    private final ReportService reportService;

    public ReportExportService(FindingRepository findingRepository,
                               BookingRepository bookingRepository,
                               SamplingRunRepository samplingRunRepository,
                               ReportService reportService) {
        this.findingRepository = findingRepository;
        this.bookingRepository = bookingRepository;
        this.samplingRunRepository = samplingRunRepository;
        this.reportService = reportService;
    }

    /**
     * Assemble full report content for a given run.
     * The run must exist; its header fields are used as the report metadata.
     */
    public ReportContent assemble(Long runId) {
        ReportRun run = reportService.findRunById(runId);

        List<Finding> findings = findingRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<SamplingRun> samplingRuns = samplingRunRepository.findAll();

        return new ReportContent(
                run.getReportName(),
                run.getTemplateVersion(),
                run.getGeneratedAt(),
                run.getTriggeredBy(),
                run.getParameters(),
                buildFindingsSummary(findings),
                buildBookingStats(bookings, findings),
                buildSamplingRunSummaries(samplingRuns)
        );
    }

    private ReportContent.FindingsSummary buildFindingsSummary(List<Finding> findings) {
        long total = findings.size();
        long high = findings.stream().filter(f -> "HIGH".equalsIgnoreCase(f.getRiskLevel())).count();
        long medium = findings.stream().filter(f -> "MEDIUM".equalsIgnoreCase(f.getRiskLevel())).count();
        long low = findings.stream().filter(f -> "LOW".equalsIgnoreCase(f.getRiskLevel())).count();
        long newCount = findings.stream().filter(f -> "NEW".equalsIgnoreCase(f.getStatus())).count();
        long escalated = findings.stream().filter(f -> "ESCALATED".equalsIgnoreCase(f.getStatus())).count();

        List<ReportContent.FindingEntry> entries = findings.stream()
                .map(f -> new ReportContent.FindingEntry(
                        f.getId(),
                        f.getBooking() != null ? f.getBooking().getId() : null,
                        f.getRuleName(),
                        f.getAlertDescription(),
                        f.getRiskLevel(),
                        f.getStatus(),
                        f.getAnalysisRunId(),
                        f.getRuleVersion(),
                        f.getCreatedAt()
                ))
                .toList();

        return new ReportContent.FindingsSummary(total, high, medium, low, newCount, escalated, entries);
    }

    private ReportContent.BookingStats buildBookingStats(List<Booking> bookings, List<Finding> findings) {
        long total = bookings.size();
        BigDecimal totalAmount = bookings.stream()
                .map(Booking::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<Long> bookingIdsWithFindings = findings.stream()
                .filter(f -> f.getBooking() != null)
                .map(f -> f.getBooking().getId())
                .collect(Collectors.toSet());
        long withFindings = bookingIdsWithFindings.size();

        return new ReportContent.BookingStats(total, totalAmount, withFindings);
    }

    private List<ReportContent.SamplingRunSummary> buildSamplingRunSummaries(List<SamplingRun> samplingRuns) {
        return samplingRuns.stream()
                .map(s -> new ReportContent.SamplingRunSummary(
                        s.getId(),
                        s.getRunName(),
                        s.getSamplingStrategy(),
                        s.getSeed(),
                        s.getPopulationSize(),
                        s.getSampleSize(),
                        s.getCreatedAt()
                ))
                .toList();
    }
}
