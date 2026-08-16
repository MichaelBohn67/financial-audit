package de.bohnottensen.financialaudit.infrastructure.web;
import de.bohnottensen.financialaudit.application.usecase.finding.FindingManagementService;
import de.bohnottensen.financialaudit.domain.model.Finding;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.core.userdetails.UserDetails; import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/findings") public class FindingRemediationApiController {
 private final FindingManagementService service; public FindingRemediationApiController(FindingManagementService s){service=s;}
 @PostMapping("/{id}/workpaper/{workpaperId}") @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')") public Finding link(@PathVariable Long id,@PathVariable Long workpaperId,@AuthenticationPrincipal UserDetails u){return service.linkWorkpaper(id,workpaperId,u.getUsername());}
 @PostMapping("/{id}/remediation") @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')") public Finding assign(@PathVariable Long id,@RequestBody Assignment r,@AuthenticationPrincipal UserDetails u){return service.assign(id,r.owner(),r.dueDate(),u.getUsername());}
 @PatchMapping("/{id}/remediation") @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')") public Finding transition(@PathVariable Long id,@RequestBody Transition r,@AuthenticationPrincipal UserDetails u){return service.transition(id,r.status(),r.comment(),u.getUsername());}
 public record Assignment(String owner,LocalDate dueDate){} public record Transition(String status,String comment){}
}
