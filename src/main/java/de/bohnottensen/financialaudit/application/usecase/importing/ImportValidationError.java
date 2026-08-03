package de.bohnottensen.financialaudit.application.usecase.importing;

public record ImportValidationError(int index, String error) {
}
