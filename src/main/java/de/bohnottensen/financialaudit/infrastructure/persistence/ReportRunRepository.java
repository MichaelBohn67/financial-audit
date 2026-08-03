package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReportRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRunRepository extends JpaRepository<ReportRun, Long> {
}
