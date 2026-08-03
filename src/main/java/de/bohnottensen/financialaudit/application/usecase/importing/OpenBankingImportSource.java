package de.bohnottensen.financialaudit.application.usecase.importing;

public record OpenBankingImportSource(String tenantId, String projectId, String accountId) {

    public OpenBankingImportSource {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
    }
}
