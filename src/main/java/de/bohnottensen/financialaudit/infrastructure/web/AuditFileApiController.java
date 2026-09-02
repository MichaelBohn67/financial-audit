package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.*;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-files")
public class AuditFileApiController {
    private final AuditEngagementRepository engagements;
    private final AuditRiskRepository risks;
    private final AuditProcedureRepository procedures;
    private final WorkpaperRepository workpapers;
    private final WorkpaperEvidenceRepository evidence;

    public AuditFileApiController(AuditEngagementRepository engagements, AuditRiskRepository risks,
                                  AuditProcedureRepository procedures, WorkpaperRepository workpapers,
                                  WorkpaperEvidenceRepository evidence) {
        this.engagements = engagements; this.risks = risks; this.procedures = procedures;
        this.workpapers = workpapers; this.evidence = evidence;
    }

    @PostMapping("/engagements")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #request.tenantId(), #request.projectId())")
    public AuditEngagement createEngagement(@Valid @RequestBody EngagementRequest request,
                                            @AuthenticationPrincipal UserDetails user) {
        AuditEngagement value = new AuditEngagement();
        value.setTenantId(request.tenantId()); value.setProjectId(request.projectId());
        value.setEngagementKey(request.engagementKey()); value.setClientName(request.clientName());
        value.setPeriodStart(request.periodStart()); value.setPeriodEnd(request.periodEnd());
        value.setCreatedBy(user.getUsername());
        return engagements.save(value);
    }

    @GetMapping("/engagements")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public List<AuditEngagement> engagements(@RequestParam String tenantId, @RequestParam String projectId) {
        return engagements.findByTenantIdAndProjectId(tenantId, projectId);
    }

    @PostMapping("/engagements/{engagementId}/risks")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public AuditRisk createRisk(@PathVariable Long engagementId, @RequestParam String tenantId,
                                @RequestParam String projectId, @Valid @RequestBody RiskRequest request,
                                @AuthenticationPrincipal UserDetails user) {
        AuditEngagement engagement = scopedEngagement(engagementId, tenantId, projectId);
        AuditRisk value = new AuditRisk(); value.setEngagement(engagement); value.setRiskCode(request.riskCode());
        value.setTitle(request.title()); value.setDescription(request.description()); value.setAssertion(request.assertion());
        value.setInherentRisk(request.inherentRisk()); value.setControlRisk(request.controlRisk()); value.setResponse(request.response());
        value.setCreatedBy(user.getUsername()); return risks.save(value);
    }

    @GetMapping("/engagements/{engagementId}/risks")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public List<AuditRisk> risks(@PathVariable Long engagementId, @RequestParam String tenantId, @RequestParam String projectId) {
        scopedEngagement(engagementId, tenantId, projectId); return risks.findByEngagementId(engagementId);
    }

    @PostMapping("/engagements/{engagementId}/procedures")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public AuditProcedure createProcedure(@PathVariable Long engagementId, @RequestParam String tenantId,
                                          @RequestParam String projectId, @Valid @RequestBody ProcedureRequest request) {
        AuditEngagement engagement = scopedEngagement(engagementId, tenantId, projectId);
        AuditProcedure value = new AuditProcedure(); value.setEngagement(engagement); value.setProcedureCode(request.procedureCode());
        value.setTitle(request.title()); value.setObjective(request.objective()); value.setProcedureType(request.procedureType());
        if (request.riskId() != null) value.setRisk(risks.findById(request.riskId()).orElseThrow());
        if (request.workpaperId() != null) value.setWorkpaper(workpapers.findById(request.workpaperId()).orElseThrow());
        return procedures.save(value);
    }

    @GetMapping("/engagements/{engagementId}/procedures")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public List<AuditProcedure> procedures(@PathVariable Long engagementId, @RequestParam String tenantId, @RequestParam String projectId) {
        scopedEngagement(engagementId, tenantId, projectId); return procedures.findByEngagementId(engagementId);
    }

    @PostMapping("/workpapers/{workpaperId}/evidence")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public WorkpaperEvidence addEvidence(@PathVariable Long workpaperId, @RequestParam String tenantId,
                                         @RequestParam String projectId, @Valid @RequestBody EvidenceRequest request,
                                         @AuthenticationPrincipal UserDetails user) {
        Workpaper workpaper = workpapers.findById(workpaperId).orElseThrow();
        WorkpaperEvidence value = new WorkpaperEvidence(); value.setWorkpaper(workpaper);
        value.setEvidenceType(request.evidenceType()); value.setFileName(request.fileName());
        value.setReferenceUri(request.referenceUri()); value.setSha256(request.sha256()); value.setMimeType(request.mimeType());
        value.setUploadedBy(user.getUsername()); return evidence.save(value);
    }

    @GetMapping("/workpapers/{workpaperId}/evidence")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public List<WorkpaperEvidence> evidence(@PathVariable Long workpaperId, @RequestParam String tenantId, @RequestParam String projectId) {
        return evidence.findByWorkpaperId(workpaperId);
    }

    private AuditEngagement scopedEngagement(Long id, String tenantId, String projectId) {
        AuditEngagement value = engagements.findById(id).orElseThrow();
        if (!tenantId.equals(value.getTenantId()) || !projectId.equals(value.getProjectId())) throw new IllegalArgumentException("Audit engagement is outside requested scope");
        return value;
    }
    public record EngagementRequest(@NotBlank String tenantId, @NotBlank String projectId, @NotBlank String engagementKey,
                                     @NotBlank String clientName, @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd) {}
    public record RiskRequest(@NotBlank String riskCode, @NotBlank String title, @NotBlank String description, String assertion,
                              @NotBlank String inherentRisk, @NotBlank String controlRisk, String response) {}
    public record ProcedureRequest(@NotBlank String procedureCode, @NotBlank String title, @NotBlank String objective,
                                   @NotBlank String procedureType, Long riskId, Long workpaperId) {}
    public record EvidenceRequest(@NotBlank String evidenceType, @NotBlank String fileName, @NotBlank String referenceUri,
                                  @NotBlank String sha256, String mimeType) {}
}
