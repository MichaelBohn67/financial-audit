package de.bohnottensen.financialaudit.infrastructure.web;
import de.bohnottensen.financialaudit.application.usecase.materiality.MaterialityService;
import de.bohnottensen.financialaudit.domain.model.MaterialityConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@RestController @RequestMapping("/api/materiality")
public class MaterialityApiController {
 private final MaterialityService service; public MaterialityApiController(MaterialityService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')") public MaterialityConfig active(){return service.active();}
 @PostMapping @PreAuthorize("hasAnyRole('LEAD_AUDITOR','ADMIN')") public MaterialityConfig save(@RequestBody Request r){return service.save(r.name(),r.overall(),r.performance(),r.deMinimis());}
 @GetMapping("/accounts/{account}") @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')") public Object evaluate(@PathVariable String account){return service.evaluateAll(account);}
 public record Request(String name, BigDecimal overall, BigDecimal performance, BigDecimal deMinimis){}
}
