package de.bohnottensen.financialaudit.infrastructure.web;
import de.bohnottensen.financialaudit.application.usecase.finding.FindingManagementService;
import de.bohnottensen.financialaudit.domain.model.Finding;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.core.userdetails.UserDetails; import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/findings") public class FindingRemediationApiController {
 private final FindingManagementService service; public FindingRemediationApiController(FindingManagementService s){service=s;}
 @PostMapping("/{id}/workpaper/{workpaperId}") @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)") public Finding link(@PathVariable Long id,@PathVariable Long workpaperId,@RequestParam String tenantId,@RequestParam String projectId,@AuthenticationPrincipal UserDetails u){return service.linkWorkpaper(id,workpaperId,tenantId,projectId,u.getUsername());}
 @PostMapping("/{id}/remediation") @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)") public Finding assign(@PathVariable Long id,@RequestBody Assignment r,@RequestParam String tenantId,@RequestParam String projectId,@AuthenticationPrincipal UserDetails u){return service.assign(id,r.owner(),r.dueDate(),tenantId,projectId,u.getUsername());}
 @PatchMapping("/{id}/remediation") @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)") public Finding transition(@PathVariable Long id,@RequestBody Transition r,@RequestParam String tenantId,@RequestParam String projectId,@AuthenticationPrincipal UserDetails u){return service.transition(id,r.status(),r.comment(),tenantId,projectId,u.getUsername());}
 @PatchMapping("/{id}/remediation/plan") @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)") public Finding updatePlan(@PathVariable Long id,@RequestBody Plan r,@RequestParam String tenantId,@RequestParam String projectId,@AuthenticationPrincipal UserDetails u){return service.updatePlan(id,r.plan(),tenantId,projectId,u.getUsername());}
 public record Assignment(String owner,LocalDate dueDate){} public record Transition(String status,String comment){} public record Plan(String plan){}
}
