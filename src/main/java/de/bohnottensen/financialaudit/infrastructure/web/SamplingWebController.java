package de.bohnottensen.financialaudit.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SamplingWebController {
    @GetMapping("/sampling")
    @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')")
    public String sampling() {
        return "sampling";
    }
}
