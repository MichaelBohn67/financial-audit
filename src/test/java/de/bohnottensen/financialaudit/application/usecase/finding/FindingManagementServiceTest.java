package de.bohnottensen.financialaudit.application.usecase.finding;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FindingManagementServiceTest {

    private FindingRepository findings;
    private WorkpaperRepository workpapers;
    private AuditTrailWriter audit;
    private FindingManagementService service;

    @BeforeEach
    void setUp() {
        findings = mock(FindingRepository.class);
        workpapers = mock(WorkpaperRepository.class);
        audit = mock(AuditTrailWriter.class);
        when(findings.save(any(Finding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new FindingManagementService(findings, workpapers, audit);
    }

    @Test
    void shouldRequireRemediationFieldsBeforeReview() {
        Finding f = finding("OPEN");
        when(findings.findById(1L)).thenReturn(Optional.of(f));

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
        Finding finding = finding("OPEN");
        when(findings.findById(1L)).thenReturn(Optional.of(finding));

        Finding assigned = service.assign(1L, "owner", LocalDate.of(2026, 9, 1), "auditor");
        assertThat(assigned).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationOwner()).isEqualTo("owner");
        assertThat(finding.getRemediationDueDate()).isEqualTo(LocalDate.of(2026, 9, 1));

        Finding planned = service.updatePlan(1L, "Implement control", "auditor");
        assertThat(planned).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationPlan()).isEqualTo("Implement control");

        Finding inProgress = service.transition(1L, "IN_PROGRESS", null, "auditor");
        assertThat(inProgress).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationStatus()).isEqualTo("IN_PROGRESS");

        Finding ready = service.transition(1L, "READY_FOR_REVIEW", null, "auditor");
        assertThat(ready).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationStatus()).isEqualTo("READY_FOR_REVIEW");

        assertThatThrownBy(() -> service.transition(1L, "RESOLVED", "", "reviewer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution comment");

        assertThatThrownBy(() -> service.transition(1L, "RESOLVED", "   ", "reviewer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution comment");

        Finding resolved = service.transition(1L, "RESOLVED", "Control implemented", "reviewer");
        assertThat(resolved).isNotNull().isSameAs(finding);
        assertThat(finding.getResolutionComment()).isEqualTo("Control implemented");
        assertThat(finding.getResolvedBy()).isEqualTo("reviewer");
        assertThat(finding.getResolvedAt()).isNotNull();

        Finding closed = service.transition(1L, "CLOSED", null, "reviewer");
        assertThat(closed).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationStatus()).isEqualTo("CLOSED");

        assertThatThrownBy(() -> service.updatePlan(1L, "Another plan", "reviewer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Closed findings");

        assertThatThrownBy(() -> service.assign(1L, "new-owner", LocalDate.now(), "reviewer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Closed findings");

        assertThatThrownBy(() -> service.linkWorkpaper(1L, 99L, "reviewer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Closed findings");

        assertThatThrownBy(() -> service.transition(1L, "IN_PROGRESS", null, "reviewer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid remediation transition: CLOSED -> IN_PROGRESS");
    }

    @Test
    void shouldSupportRejectTransitionAndReopen() {
        Finding finding = finding("OPEN");
        when(findings.findById(1L)).thenReturn(Optional.of(finding));

        service.assign(1L, "owner", LocalDate.of(2026, 9, 1), "auditor");
        service.updatePlan(1L, "Implement control", "auditor");
        service.transition(1L, "IN_PROGRESS", null, "auditor");
        service.transition(1L, "READY_FOR_REVIEW", null, "auditor");

        assertThatThrownBy(() -> service.transition(1L, "REJECTED", null, "reviewer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution comment is required");

        assertThatThrownBy(() -> service.transition(1L, "REJECTED", "   ", "reviewer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution comment is required");

        Finding rejected = service.transition(1L, "REJECTED", "Evidence insufficient", "reviewer");
        assertThat(rejected).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationStatus()).isEqualTo("REJECTED");
        assertThat(finding.getResolutionComment()).isEqualTo("Evidence insufficient");
        assertThat(finding.getResolvedAt()).isNull();
        assertThat(finding.getResolvedBy()).isNull();

        Finding reopen = service.transition(1L, "IN_PROGRESS", null, "auditor");
        assertThat(reopen).isNotNull().isSameAs(finding);
        assertThat(finding.getRemediationStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldLinkWorkpaperToFinding() {
        Finding finding = finding("OPEN");
        when(findings.findById(1L)).thenReturn(Optional.of(finding));

        Workpaper workpaper = new Workpaper();
        workpaper.setId(50L);
        when(workpapers.findById(50L)).thenReturn(Optional.of(workpaper));

        Finding result = service.linkWorkpaper(1L, 50L, "auditor");

        assertThat(result).isNotNull().isSameAs(finding);
        assertThat(result.getWorkpaper()).isSameAs(workpaper);

        verify(audit).record(
                eq("FINDING"),
                eq(1L),
                eq("LINK_WORKPAPER"),
                eq("auditor"),
                eq("Finding remediation changed"),
                anyString(),
                contains("workpaperId=50")
        );
    }

    @Test
    void shouldValidateInputsAndActors() {
        Finding f = finding("OPEN");
        when(findings.findById(1L)).thenReturn(Optional.of(f));

        assertThat(service.get(1L)).isSameAs(f);

        assertThatThrownBy(() -> service.get(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Finding id is required");

        assertThatThrownBy(() -> service.assign(1L, " ", LocalDate.now(), "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.assign(1L, "owner", null, "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.assign(1L, "owner", LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.assign(1L, "owner", LocalDate.now(), "  "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.updatePlan(1L, null, "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updatePlan(1L, " ", "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updatePlan(1L, "Valid plan", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updatePlan(1L, "Valid plan", "  "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.linkWorkpaper(1L, null, "auditor"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.linkWorkpaper(1L, 50L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.linkWorkpaper(1L, 50L, "  "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.transition(1L, "NOT_A_STATUS", null, "auditor"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.transition(1L, "IN_PROGRESS", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transition(1L, "IN_PROGRESS", null, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Finding finding(String status) {
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setRemediationStatus(status);
        return finding;
    }
}
