package de.bohnottensen.financialaudit.application.usecase.importing;

import java.time.LocalDateTime;
import java.util.List;

public record ImportJobResult(
        String sourceType,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int importedCount,
        int invalidCount,
        List<ImportValidationError> validationErrors,
        String checksum
) {
}
