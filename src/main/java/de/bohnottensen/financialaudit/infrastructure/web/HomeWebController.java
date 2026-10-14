package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.infrastructure.web.crud.DomainModelCrudService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    private final DomainModelCrudService crudService;

    public HomeWebController(DomainModelCrudService crudService) {
        this.crudService = crudService;
    }

    @GetMapping("/")
    public String home(Model model) {
        if (crudService != null) {
            model.addAttribute("crudModels", crudService.getAllDescriptors());
        }
        return "index";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')")
    public String dashboard() { return "dashboard"; }
}
