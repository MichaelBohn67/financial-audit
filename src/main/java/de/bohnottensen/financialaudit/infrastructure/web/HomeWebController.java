package de.bohnottensen.financialaudit.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')")
    public String dashboard() { return "dashboard"; }
}
