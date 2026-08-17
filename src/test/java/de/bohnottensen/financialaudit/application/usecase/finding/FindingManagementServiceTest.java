package de.bohnottensen.financialaudit.application.usecase.finding;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindingManagementServiceTest {

    @Test
    void shouldRequireRemediationFieldsBeforeReview() {
        FindingRepository findings = mock(FindingRepository.class);
        when(findings.findById(1L)).thenReturn(Optional.of(finding("OPEN")));
        FindingManagementService service = service(findings);

        assertThatThrownBy(() -> service.transition(1L, "IN_PROGRESS", null, "auditor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Owner and due date");

        service.assign(1L, "owner", LocalDate.of(2026, 9, 1), "auditor");
        service.transition(1L, "IN_PROGRESS", null, "auditor");
        assertThatThrownBy(() -> service.transition(1L, "READY_FOR_REVIEW", null, "auditor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Remediation plan");
    }

    @Test
    void shouldEnforceLifecycleAndResolutionComment() {
        FindingRepository findings = mock(FindingRepository.class);
        Finding finding = finding("OPEN");
        when(findings.findById(1L)).thenReturn(Optional.of(finding));
        when(findings.save(any(Finding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FindingManagementService service = service(findings);

        service.assign(1L, "owner", LocalDate.of(2026, 9, 1), "auditor");
        service.updatePlan(1L, "Implement control", "auditor");
        service.transition(1L, "IN_PROGRESS", null, "auditor");
        service.transition(1L, "READY_FOR_REVIEW", null, "auditor");

        assertThatThrownBy(() -> service.transition(1L, "RESOLVED", "", "reviewer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution comment");

        service.transition(1L, "RESOLVED", "Control implemented", "reviewer");
        assertThat(finding.getResolvedBy()).isEqualTo("reviewer");
        assertThat(finding.getResolvedAt()).isNotNull();
        service.transition(1L, "CLOSED", null, "reviewer");
        assertThat(finding.getRemediationStatus()).isEqualTo("CLOSED");

        assertThatThrownBy(() -> service.updatePlan(1L, "Another plan", "reviewer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Closed findings");
    }

    @Test
    void shouldRejectInvalidAssignmentsAndStatuses() {
        FindingRepository findings = mock(FindingRepository.class);
        when(findings.findById(1L)).thenReturn(Optional.of(finding("OPEN")));
        FindingManagementService service = service(findings);

        assertThatThrownBy(() -> service.assign(1L, " ", LocalDate.now(), "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.assign(1L, "owner", null, "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transition(1L, "NOT_A_STATUS", null, "auditor"))
                .isInstanceOf(IllegalStateException.class);
    }

    private FindingManagementService service(FindingRepository findings) {
        when(findings.save(any(Finding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new FindingManagementService(findings, mock(WorkpaperRepository.class),
                mock(AuditTrailWriter.class));
    }

    private Finding finding(String status) {
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setRemediationStatus(status);
        return finding;
    }
}
