package de.bohnottensen.financialaudit.application.usecase.finding;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FindingAuditRecorderTest {

    private final FindingRepository findingRepository = mock(FindingRepository.class);
    private final AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);
    private final FindingAuditRecorder recorder =
            new FindingAuditRecorder(findingRepository, auditTrailWriter);

    @Test
    void shouldDefaultStatusToNewAndRecordAuditEntry() {
        Booking booking = new Booking();
        booking.setId(7L);

        Finding finding = new Finding();
        finding.setBooking(booking);
        finding.setRuleName("AML_RULE");
        finding.setRiskLevel("HIGH");
        finding.setAnalysisRunId("ANL-1");
        finding.setRuleVersion("v1");
        finding.setRunContext("ctx");

        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        Finding saved = recorder.record(finding, "SYSTEM_AML", "Finding created by AML analysis");

        assertThat(saved.getId()).isEqualTo(500L);
        assertThat(saved.getStatus()).isEqualTo("NEW");

        ArgumentCaptor<String> snapshotCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter).record(
                eq("FINDING"),
                eq(500L),
                eq("FINDING_CREATED"),
                eq("SYSTEM_AML"),
                eq("Finding created by AML analysis"),
                isNull(),
                snapshotCaptor.capture()
        );

        assertThat(snapshotCaptor.getValue()).isEqualTo(
                "bookingId=7;ruleName=AML_RULE;riskLevel=HIGH;status=NEW;analysisRunId=ANL-1;ruleVersion=v1;runContext=ctx");
    }

    @Test
    void shouldTolerateFindingWithoutBooking() {
        Finding finding = new Finding();
        finding.setRuleName("PATTERN_GAP");
        finding.setRiskLevel("LOW");

        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(recorder.snapshot(finding))
                .startsWith("bookingId=null;ruleName=PATTERN_GAP");
    }

    @Test
    void shouldKeepAnAlreadyAssignedStatus() {
        Finding finding = new Finding();
        finding.setRuleName("RULE");
        finding.setStatus("ESCALATED");

        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(recorder.record(finding, "SYSTEM", "summary").getStatus()).isEqualTo("ESCALATED");
    }
}
