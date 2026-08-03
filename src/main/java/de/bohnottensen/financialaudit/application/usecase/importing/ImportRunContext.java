package de.bohnottensen.financialaudit.application.usecase.importing;

public record ImportRunContext(String tenantId, String projectId, String documentId, String contextLabel) {

    public ImportRunContext {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
    }

    public String asProtocolContext() {
        return "tenantId=" + tenantId
                + ";projectId=" + projectId
                + ";documentId=" + documentId
                + ";label=" + (contextLabel == null || contextLabel.isBlank() ? "default" : contextLabel);
    }
}
