package de.bohnottensen.financialaudit.infrastructure.web;
import de.bohnottensen.financialaudit.application.usecase.dashboard.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/dashboard") public class DashboardApiController { private final DashboardService service; public DashboardApiController(DashboardService s){service=s;} @GetMapping @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')") public DashboardService.Metrics metrics(){return service.metrics();} }
