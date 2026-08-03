package de.bohnottensen.financialaudit.application.usecase.analytics;

public record AnalysisRunRequest(String ruleVersion, String runContext) {
}
