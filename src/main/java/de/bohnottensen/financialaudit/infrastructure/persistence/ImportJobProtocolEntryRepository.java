package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ImportJobProtocolEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobProtocolEntryRepository extends JpaRepository<ImportJobProtocolEntry, Long> {
}
