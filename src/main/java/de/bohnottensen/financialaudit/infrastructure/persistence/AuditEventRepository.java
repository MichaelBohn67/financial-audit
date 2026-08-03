package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
