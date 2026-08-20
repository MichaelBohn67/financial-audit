package de.bohnottensen.financialaudit.application.usecase.reporting;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.ReportRunStatus;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportExportServiceTest {

    private final FindingRepository findingRepo = mock(FindingRepository.class);
    private final BookingRepository bookingRepo = mock(BookingRepository.class);
    private final SamplingRunRepository samplingRunRepo = mock(SamplingRunRepository.class);
    private final ReportService reportService = mock(ReportService.class);

    private final ReportExportService service = new ReportExportService(
            findingRepo, bookingRepo, samplingRunRepo, reportService);

    @Test
    void shouldAssembleReportWithFindingsSummaryAndBookingStats() {
        ReportRun run = runningRun(1L, "AML-Report", "1.0.0");
        when(reportService.findRunById(1L)).thenReturn(run);

        Booking booking1 = booking(10L, new BigDecimal("5000.00"));
        Booking booking2 = booking(11L, new BigDecimal("2500.00"));
        Booking bookingNullAmount = booking(12L, null);

        Finding finding1 = finding(100L, booking1, "HIGH_AMOUNT", "HIGH", "NEW");
        Finding finding2 = finding(101L, booking2, "STRUCTURING", "MEDIUM", "ESCALATED");
        Finding finding3 = finding(102L, null, "MISSING_DOC", "LOW", "NEW");

        when(findingRepo.findAll()).thenReturn(List.of(finding1, finding2, finding3));
        when(bookingRepo.findAll()).thenReturn(List.of(booking1, booking2, bookingNullAmount));
        when(samplingRunRepo.findAll()).thenReturn(List.of());

        ReportContent content = service.assemble(1L);

        assertThat(content.reportName()).isEqualTo("AML-Report");
        assertThat(content.templateVersion()).isEqualTo("1.0.0");

        ReportContent.FindingsSummary summary = content.findingsSummary();
        assertThat(summary.totalFindings()).isEqualTo(3);
        assertThat(summary.highRisk()).isEqualTo(1);
        assertThat(summary.mediumRisk()).isEqualTo(1);
        assertThat(summary.lowRisk()).isEqualTo(1);
        assertThat(summary.newFindings()).isEqualTo(2);
        assertThat(summary.escalatedFindings()).isEqualTo(1);
        assertThat(summary.entries()).hasSize(3);
        assertThat(summary.entries().get(0).bookingId()).isEqualTo(10L);
        assertThat(summary.entries().get(1).bookingId()).isEqualTo(11L);
        assertThat(summary.entries().get(2).bookingId()).isNull();

        ReportContent.BookingStats stats = content.bookingStats();
        assertThat(stats.totalBookings()).isEqualTo(3);
        assertThat(stats.totalAmount()).isEqualByComparingTo(new BigDecimal("7500.00"));
        assertThat(stats.bookingsWithFindings()).isEqualTo(2);
    }

    @Test
    void shouldIncludeSamplingRunSummaries() {
        ReportRun run = runningRun(2L, "Sampling-Report", "1.0.0");
        when(reportService.findRunById(2L)).thenReturn(run);
        when(findingRepo.findAll()).thenReturn(List.of());
        when(bookingRepo.findAll()).thenReturn(List.of());

        SamplingRun sr = new SamplingRun();
        sr.setId(7L);
        sr.setRunName("Q1-MUS");
        sr.setSamplingStrategy("MUS");
        sr.setSeed(42L);
        sr.setPopulationSize(1000L);
        sr.setSampleSize(50L);
        when(samplingRunRepo.findAll()).thenReturn(List.of(sr));

        ReportContent content = service.assemble(2L);

        assertThat(content.samplingRuns()).hasSize(1);
        ReportContent.SamplingRunSummary srSummary = content.samplingRuns().get(0);
        assertThat(srSummary.runName()).isEqualTo("Q1-MUS");
        assertThat(srSummary.samplingStrategy()).isEqualTo("MUS");
        assertThat(srSummary.populationSize()).isEqualTo(1000L);
        assertThat(srSummary.sampleSize()).isEqualTo(50L);
    }

    @Test
    void shouldHandleEmptyDataSetDeterministically() {
        ReportRun run = runningRun(3L, "Empty-Report", "1.0.0");
        when(reportService.findRunById(3L)).thenReturn(run);
        when(findingRepo.findAll()).thenReturn(List.of());
        when(bookingRepo.findAll()).thenReturn(List.of());
        when(samplingRunRepo.findAll()).thenReturn(List.of());

        ReportContent content = service.assemble(3L);

        assertThat(content.findingsSummary().totalFindings()).isZero();
        assertThat(content.bookingStats().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(content.samplingRuns()).isEmpty();
    }

    @Test
    void shouldCountDistinctBookingsWithFindingsCorrectly() {
        ReportRun run = runningRun(4L, "AML-Report", "1.0.0");
        when(reportService.findRunById(4L)).thenReturn(run);

        Booking booking = booking(20L, new BigDecimal("1000.00"));
        // Two findings on the same booking
        Finding f1 = finding(200L, booking, "HIGH_AMOUNT", "HIGH", "NEW");
        Finding f2 = finding(201L, booking, "STRUCTURING", "MEDIUM", "NEW");

        when(findingRepo.findAll()).thenReturn(List.of(f1, f2));
        when(bookingRepo.findAll()).thenReturn(List.of(booking));
        when(samplingRunRepo.findAll()).thenReturn(List.of());

        ReportContent content = service.assemble(4L);

        // Two findings but only one distinct booking
        assertThat(content.findingsSummary().totalFindings()).isEqualTo(2);
        assertThat(content.bookingStats().bookingsWithFindings()).isEqualTo(1);
    }

    // --- helpers ---

    private ReportRun runningRun(Long id, String name, String version) {
        ReportRun run = new ReportRun();
        run.setId(id);
        run.setReportName(name);
        run.setTemplateVersion(version);
        run.setStatus(ReportRunStatus.RUNNING.name());
        return run;
    }

    private Booking booking(Long id, BigDecimal amount) {
        Booking b = new Booking();
        b.setId(id);
        b.setAmount(amount);
        b.setDescription("Test");
        b.setCurrency("EUR");
        b.setSourceAccount("SRC");
        b.setDestinationAccount("DST");
        return b;
    }

    private Finding finding(Long id, Booking booking, String ruleName, String riskLevel, String status) {
        Finding f = new Finding();
        f.setId(id);
        f.setBooking(booking);
        f.setRuleName(ruleName);
        f.setAlertDescription(ruleName + " alert");
        f.setRiskLevel(riskLevel);
        f.setStatus(status);
        f.setAnalysisRunId("RUN-1");
        f.setRuleVersion("v1");
        f.setRunContext("default");
        return f;
    }
}
