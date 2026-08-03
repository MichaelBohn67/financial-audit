package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.importing.ImportJobResult;
import de.bohnottensen.financialaudit.application.usecase.importing.ImportOrchestratorService;
import de.bohnottensen.financialaudit.application.usecase.importing.OpenBankingImportSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/imports")
public class ImportApiController {

    private final ImportOrchestratorService importOrchestratorService;

    public ImportApiController(ImportOrchestratorService importOrchestratorService) {
        this.importOrchestratorService = importOrchestratorService;
    }

    @PostMapping("/csv")
    @PreAuthorize("@scopeAccessPolicy.canAccessDocument(authentication, #tenantId, #projectId, #documentId)")
    public ImportJobResult importCsv(@RequestParam String tenantId,
                                     @RequestParam String projectId,
                                     @RequestParam String documentId,
                                     @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty");
        }
        return importOrchestratorService.importFrom(file.getInputStream());
    }

    @PostMapping("/open-banking")
    @PreAuthorize("@scopeAccessPolicy.canAccessDocument(authentication, #tenantId, #projectId, #documentId)")
    public ImportJobResult importOpenBanking(@RequestParam String tenantId,
                                             @RequestParam String projectId,
                                             @RequestParam String documentId,
                                             @RequestParam(required = false) String accountId) {
        return importOrchestratorService.importFrom(new OpenBankingImportSource(tenantId, projectId, accountId));
    }
}
