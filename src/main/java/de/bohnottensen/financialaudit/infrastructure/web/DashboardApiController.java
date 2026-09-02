package de.bohnottensen.financialaudit.infrastructure.web;
import de.bohnottensen.financialaudit.application.usecase.dashboard.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/dashboard") public class DashboardApiController { private final DashboardService service; public DashboardApiController(DashboardService s){service=s;} @GetMapping @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)") public DashboardService.Metrics metrics(@RequestParam String tenantId, @RequestParam String projectId){return service.metrics(tenantId, projectId);} }
