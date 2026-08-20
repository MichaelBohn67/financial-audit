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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatternAnalysisServiceTest {

    @Test
    void shouldPersistPatternIssuesForDuplicateGapAndRepeatedPattern() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        PatternAnalysisRunRepository runRepository = mock(PatternAnalysisRunRepository.class);
        PatternAnalysisIssueRepository issueRepository = mock(PatternAnalysisIssueRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Booking duplicateA = booking(1L, 100L, "A", "10.00", "2026-08-01T10:00:00", "DE111", "DE222");
        Booking duplicateB = booking(2L, 100L, "B", "11.00", "2026-08-01T11:00:00", "DE333", "DE444");
        Booking gapBooking = booking(3L, 103L, "C", "12.00", "2026-08-01T12:00:00", "DE555", "DE666");
        Booking pattern1 = booking(4L, 200L, "P1", "999.00", "2026-08-01T08:00:00", "DE777", "DE888");
        Booking pattern2 = booking(5L, 201L, "P2", "999.00", "2026-08-01T09:00:00", "DE777", "DE888");
        Booking pattern3 = booking(6L, 202L, "P3", "999.00", "2026-08-01T10:00:00", "DE777", "DE888");

        when(bookingRepository.findAll()).thenReturn(List.of(duplicateA, duplicateB, gapBooking, pattern1, pattern2, pattern3));

        AtomicLong runIds = new AtomicLong(77L);
        when(runRepository.save(any(PatternAnalysisRun.class))).thenAnswer(invocation -> {
            PatternAnalysisRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(runIds.getAndIncrement());
            }
            return run;
        });
        when(issueRepository.save(any(PatternAnalysisIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding finding = invocation.getArgument(0);
            finding.setId(77L);
            return finding;
        });

        PatternAnalysisService service = new PatternAnalysisService(
                bookingRepository,
                findingRepository,
                runRepository,
                issueRepository,
                auditTrailWriter
        );

        PatternAnalysisResult result = service.run("v1", "task13");

        assertThat(result.bookingCount()).isEqualTo(6);
        assertThat(result.issueCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.runDbId()).isEqualTo(77L);
        assertThat(result.ruleVersion()).isEqualTo("v1");
        assertThat(result.runContext()).isEqualTo("task13");
        assertThat(result.runId()).startsWith("PATTERN-");
        assertThat(result.issues().stream().map(PatternAnalysisResult.IssueResult::issueType))
                .contains("DUPLICATE_DOCUMENT_NUMBER", "DOCUMENT_NUMBER_GAP", "REPEATED_TRANSFER_PATTERN");

        ArgumentCaptor<PatternAnalysisIssue> issueCaptor = ArgumentCaptor.forClass(PatternAnalysisIssue.class);
        verify(issueRepository, times(result.issueCount())).save(issueCaptor.capture());
        for (PatternAnalysisIssue issue : issueCaptor.getAllValues()) {
            assertThat(issue.getPatternRunId()).isEqualTo(77L);
            assertThat(issue.getSeverity()).isNotNull();
            assertThat(issue.getIssueType()).isNotNull();
            assertThat(issue.getPrimaryBookingId()).isNotNull();
        }

        ArgumentCaptor<Finding> findingCaptor = ArgumentCaptor.forClass(Finding.class);
        verify(findingRepository, times(result.issueCount())).save(findingCaptor.capture());
        for (Finding f : findingCaptor.getAllValues()) {
            assertThat(f.getRiskLevel()).isNotNull();
            assertThat(f.getStatus()).isEqualTo("NEW");
            assertThat(f.getRuleVersion()).isEqualTo("v1");
            assertThat(f.getRunContext()).isEqualTo("task13");
            assertThat(f.getAnalysisRunId()).isEqualTo(result.runId());
            assertThat(f.getRuleName()).startsWith("PATTERN_");
        }

        ArgumentCaptor<String> auditPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter, times(result.issueCount())).record(
                eq("FINDING"),
                eq(77L),
                eq("FINDING_CREATED"),
                eq("SYSTEM_PATTERN_ANALYSIS"),
                eq("Finding created by pattern analysis"),
                isNull(),
                auditPayloadCaptor.capture()
        );
        for (String payload : auditPayloadCaptor.getAllValues()) {
            assertThat(payload).contains(";riskLevel=")
                    .contains(";status=NEW")
                    .contains(";analysisRunId=" + result.runId())
                    .contains(";ruleVersion=v1")
                    .contains(";runContext=task13");
        }
    }

    @Test
    void shouldHandleEmptyAndNormalConsecutiveBookingsWithoutIssues() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        PatternAnalysisRunRepository runRepository = mock(PatternAnalysisRunRepository.class);
        PatternAnalysisIssueRepository issueRepository = mock(PatternAnalysisIssueRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Booking b1 = booking(1L, 100L, "A", "10.00", "2026-08-01T10:00:00", "DE111", "DE222");
        Booking b2 = booking(2L, 101L, "B", "11.00", "2026-08-05T10:00:00", "DE111", "DE222");
        Booking b3WithNulls = new Booking();
        b3WithNulls.setId(3L);

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3WithNulls));
        when(runRepository.save(any(PatternAnalysisRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatternAnalysisService service = new PatternAnalysisService(
                bookingRepository,
                findingRepository,
                runRepository,
                issueRepository,
                auditTrailWriter
        );

        PatternAnalysisResult result = service.run("v1", "clean-test");

        assertThat(result.issueCount()).isEqualTo(0);
        assertThat(result.issues()).isEmpty();
        verify(issueRepository, times(0)).save(any());
        verify(findingRepository, times(0)).save(any());
    }

    @Test
    void shouldIgnoreRepeatedTransfersSpanGreaterThan24Hours() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FindingRepository findingRepository = mock(FindingRepository.class);
        PatternAnalysisRunRepository runRepository = mock(PatternAnalysisRunRepository.class);
        PatternAnalysisIssueRepository issueRepository = mock(PatternAnalysisIssueRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Booking b1 = booking(1L, 100L, "P1", "50.00", "2026-08-01T10:00:00", "DE111", "DE222");
        Booking b2 = booking(2L, 101L, "P2", "50.00", "2026-08-02T15:00:00", "DE111", "DE222");
        Booking b3 = booking(3L, 102L, "P3", "50.00", "2026-08-03T20:00:00", "DE111", "DE222");

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));
        when(runRepository.save(any(PatternAnalysisRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatternAnalysisService service = new PatternAnalysisService(
                bookingRepository,
                findingRepository,
                runRepository,
                issueRepository,
                auditTrailWriter
        );

        PatternAnalysisResult result = service.run("v1", "span-test");

        assertThat(result.issueCount()).isEqualTo(0);
        verify(findingRepository, times(0)).save(any());
    }

    private Booking booking(Long id,
                            Long foreignTransactionId,
                            String description,
                            String amount,
                            String timestamp,
                            String sourceAccount,
                            String destinationAccount) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setForeignTransactionId(foreignTransactionId);
        booking.setDescription(description);
        booking.setAmount(new BigDecimal(amount));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.parse(timestamp));
        booking.setSourceAccount(sourceAccount);
        booking.setDestinationAccount(destinationAccount);
        return booking;
    }
}
