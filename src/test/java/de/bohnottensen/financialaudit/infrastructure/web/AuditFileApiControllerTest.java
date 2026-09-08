package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.*;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditFileApiControllerTest {

    private AuditEngagementRepository engagements;
    private AuditRiskRepository risks;
    private AuditProcedureRepository procedures;
    private WorkpaperRepository workpapers;
    private WorkpaperEvidenceRepository evidence;
    private AuditFileApiController controller;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        engagements = mock(AuditEngagementRepository.class);
        risks = mock(AuditRiskRepository.class);
        procedures = mock(AuditProcedureRepository.class);
        workpapers = mock(WorkpaperRepository.class);
        evidence = mock(WorkpaperEvidenceRepository.class);
        controller = new AuditFileApiController(engagements, risks, procedures, workpapers, evidence);

        user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("lead_auditor");
    }

    @Test
    void shouldCreateEngagement() {
        AuditFileApiController.EngagementRequest request = new AuditFileApiController.EngagementRequest(
                "TENANT-1", "PROJECT-1", "ENG-2026", "ACME Corp",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );

        when(engagements.save(any(AuditEngagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEngagement result = controller.createEngagement(request, user);

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("TENANT-1");
        assertThat(result.getProjectId()).isEqualTo("PROJECT-1");
        assertThat(result.getEngagementKey()).isEqualTo("ENG-2026");
        assertThat(result.getClientName()).isEqualTo("ACME Corp");
        assertThat(result.getPeriodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.getCreatedBy()).isEqualTo("lead_auditor");

        verify(engagements).save(any(AuditEngagement.class));
    }

    @Test
    void shouldFindEngagements() {
        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findByTenantIdAndProjectId("TENANT-1", "PROJECT-1")).thenReturn(List.of(engagement));

        List<AuditEngagement> result = controller.engagements("TENANT-1", "PROJECT-1");

        assertThat(result).containsExactly(engagement);
        verify(engagements).findByTenantIdAndProjectId("TENANT-1", "PROJECT-1");
    }

    @Test
    void shouldCreateRiskForScopedEngagement() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));
        when(risks.save(any(AuditRisk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditFileApiController.RiskRequest request = new AuditFileApiController.RiskRequest(
                "RSK-01", "Revenue Recognition", "Risk of early revenue recognition",
                "Completeness", "HIGH", "MEDIUM", "Substantive testing"
        );

        AuditRisk result = controller.createRisk(10L, "TENANT-1", "PROJECT-1", request, user);

        assertThat(result.getEngagement()).isEqualTo(engagement);
        assertThat(result.getRiskCode()).isEqualTo("RSK-01");
        assertThat(result.getTitle()).isEqualTo("Revenue Recognition");
        assertThat(result.getDescription()).isEqualTo("Risk of early revenue recognition");
        assertThat(result.getAssertion()).isEqualTo("Completeness");
        assertThat(result.getInherentRisk()).isEqualTo("HIGH");
        assertThat(result.getControlRisk()).isEqualTo("MEDIUM");
        assertThat(result.getResponse()).isEqualTo("Substantive testing");
        assertThat(result.getCreatedBy()).isEqualTo("lead_auditor");

        verify(risks).save(any(AuditRisk.class));
    }

    @Test
    void shouldFindRisksForScopedEngagement() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));

        AuditRisk risk = new AuditRisk();
        risk.setRiskCode("RSK-01");
        when(risks.findByEngagementId(10L)).thenReturn(List.of(risk));

        List<AuditRisk> result = controller.risks(10L, "TENANT-1", "PROJECT-1");

        assertThat(result).containsExactly(risk);
        verify(risks).findByEngagementId(10L);
    }

    @Test
    void shouldThrowWhenScopedEngagementNotFound() {
        when(engagements.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.risks(99L, "TENANT-1", "PROJECT-1"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldThrowWhenTenantIdDoesNotMatchScopedEngagement() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("OTHER-TENANT");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));

        assertThatThrownBy(() -> controller.risks(10L, "TENANT-1", "PROJECT-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audit engagement is outside requested scope");
    }

    @Test
    void shouldThrowWhenProjectIdDoesNotMatchScopedEngagement() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("OTHER-PROJECT");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));

        assertThatThrownBy(() -> controller.risks(10L, "TENANT-1", "PROJECT-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audit engagement is outside requested scope");
    }

    @Test
    void shouldCreateProcedureWithRiskAndWorkpaper() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));

        AuditRisk risk = new AuditRisk();
        ReflectionTestUtils.setField(risk, "id", 20L);
        when(risks.findById(20L)).thenReturn(Optional.of(risk));

        Workpaper workpaper = new Workpaper();
        workpaper.setId(30L);
        when(workpapers.findById(30L)).thenReturn(Optional.of(workpaper));

        when(procedures.save(any(AuditProcedure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditFileApiController.ProcedureRequest request = new AuditFileApiController.ProcedureRequest(
                "PRC-01", "Vouch Sample", "Vouch sample of invoices", "SUBSTANTIVE", 20L, 30L
        );

        AuditProcedure result = controller.createProcedure(10L, "TENANT-1", "PROJECT-1", request);

        assertThat(result.getEngagement()).isEqualTo(engagement);
        assertThat(result.getProcedureCode()).isEqualTo("PRC-01");
        assertThat(result.getTitle()).isEqualTo("Vouch Sample");
        assertThat(result.getObjective()).isEqualTo("Vouch sample of invoices");
        assertThat(result.getProcedureType()).isEqualTo("SUBSTANTIVE");
        assertThat(result.getRisk()).isEqualTo(risk);
        assertThat(result.getWorkpaper()).isEqualTo(workpaper);

        verify(procedures).save(any(AuditProcedure.class));
    }

    @Test
    void shouldCreateProcedureWithoutRiskAndWorkpaper() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));

        when(procedures.save(any(AuditProcedure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditFileApiController.ProcedureRequest request = new AuditFileApiController.ProcedureRequest(
                "PRC-02", "Inquire Mgmt", "Inquiry with management", "INQUIRY", null, null
        );

        AuditProcedure result = controller.createProcedure(10L, "TENANT-1", "PROJECT-1", request);

        assertThat(result.getEngagement()).isEqualTo(engagement);
        assertThat(result.getProcedureCode()).isEqualTo("PRC-02");
        assertThat(result.getTitle()).isEqualTo("Inquire Mgmt");
        assertThat(result.getObjective()).isEqualTo("Inquiry with management");
        assertThat(result.getProcedureType()).isEqualTo("INQUIRY");
        assertThat(result.getRisk()).isNull();
        assertThat(result.getWorkpaper()).isNull();

        verify(procedures).save(any(AuditProcedure.class));
    }

    @Test
    void shouldFindProceduresForScopedEngagement() {
        AuditEngagement engagement = new AuditEngagement();
        ReflectionTestUtils.setField(engagement, "id", 10L);
        engagement.setTenantId("TENANT-1");
        engagement.setProjectId("PROJECT-1");
        when(engagements.findById(10L)).thenReturn(Optional.of(engagement));

        AuditProcedure procedure = new AuditProcedure();
        procedure.setProcedureCode("PRC-01");
        when(procedures.findByEngagementId(10L)).thenReturn(List.of(procedure));

        List<AuditProcedure> result = controller.procedures(10L, "TENANT-1", "PROJECT-1");

        assertThat(result).containsExactly(procedure);
        verify(procedures).findByEngagementId(10L);
    }

    @Test
    void shouldAddEvidenceToWorkpaper() {
        Workpaper workpaper = new Workpaper();
        workpaper.setId(30L);
        when(workpapers.findById(30L)).thenReturn(Optional.of(workpaper));

        when(evidence.save(any(WorkpaperEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditFileApiController.EvidenceRequest request = new AuditFileApiController.EvidenceRequest(
                "DOCUMENT", "invoice_123.pdf", "s3://bucket/invoice_123.pdf", "hash123", "application/pdf"
        );

        WorkpaperEvidence result = controller.addEvidence(30L, "TENANT-1", "PROJECT-1", request, user);

        assertThat(result.getWorkpaper()).isEqualTo(workpaper);
        assertThat(result.getEvidenceType()).isEqualTo("DOCUMENT");
        assertThat(result.getFileName()).isEqualTo("invoice_123.pdf");
        assertThat(result.getReferenceUri()).isEqualTo("s3://bucket/invoice_123.pdf");
        assertThat(result.getSha256()).isEqualTo("hash123");
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getUploadedBy()).isEqualTo("lead_auditor");

        verify(evidence).save(any(WorkpaperEvidence.class));
    }

    @Test
    void shouldThrowWhenWorkpaperNotFoundForEvidence() {
        when(workpapers.findById(99L)).thenReturn(Optional.empty());

        AuditFileApiController.EvidenceRequest request = new AuditFileApiController.EvidenceRequest(
                "DOCUMENT", "doc.pdf", "uri", "hash", "pdf"
        );

        assertThatThrownBy(() -> controller.addEvidence(99L, "TENANT-1", "PROJECT-1", request, user))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldFindEvidenceForWorkpaper() {
        WorkpaperEvidence ev = new WorkpaperEvidence();
        ev.setFileName("doc.pdf");
        when(evidence.findByWorkpaperId(30L)).thenReturn(List.of(ev));

        List<WorkpaperEvidence> result = controller.evidence(30L, "TENANT-1", "PROJECT-1");

        assertThat(result).containsExactly(ev);
        verify(evidence).findByWorkpaperId(30L);
    }

    @Test
    void shouldTestRequestRecords() {
        AuditFileApiController.EngagementRequest er = new AuditFileApiController.EngagementRequest(
                "T", "P", "K", "C", LocalDate.MIN, LocalDate.MAX
        );
        assertThat(er.tenantId()).isEqualTo("T");
        assertThat(er.projectId()).isEqualTo("P");
        assertThat(er.engagementKey()).isEqualTo("K");
        assertThat(er.clientName()).isEqualTo("C");
        assertThat(er.periodStart()).isEqualTo(LocalDate.MIN);
        assertThat(er.periodEnd()).isEqualTo(LocalDate.MAX);

        AuditFileApiController.RiskRequest rr = new AuditFileApiController.RiskRequest(
                "R", "T", "D", "A", "IR", "CR", "RES"
        );
        assertThat(rr.riskCode()).isEqualTo("R");
        assertThat(rr.title()).isEqualTo("T");
        assertThat(rr.description()).isEqualTo("D");
        assertThat(rr.assertion()).isEqualTo("A");
        assertThat(rr.inherentRisk()).isEqualTo("IR");
        assertThat(rr.controlRisk()).isEqualTo("CR");
        assertThat(rr.response()).isEqualTo("RES");

        AuditFileApiController.ProcedureRequest pr = new AuditFileApiController.ProcedureRequest(
                "P", "T", "O", "PT", 1L, 2L
        );
        assertThat(pr.procedureCode()).isEqualTo("P");
        assertThat(pr.title()).isEqualTo("T");
        assertThat(pr.objective()).isEqualTo("O");
        assertThat(pr.procedureType()).isEqualTo("PT");
        assertThat(pr.riskId()).isEqualTo(1L);
        assertThat(pr.workpaperId()).isEqualTo(2L);

        AuditFileApiController.EvidenceRequest evr = new AuditFileApiController.EvidenceRequest(
                "ET", "FN", "URI", "HASH", "MIME"
        );
        assertThat(evr.evidenceType()).isEqualTo("ET");
        assertThat(evr.fileName()).isEqualTo("FN");
        assertThat(evr.referenceUri()).isEqualTo("URI");
        assertThat(evr.sha256()).isEqualTo("HASH");
        assertThat(evr.mimeType()).isEqualTo("MIME");
    }
}
