package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.BenfordAnalysisRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenfordAnalysisRunRepository extends JpaRepository<BenfordAnalysisRun, Long> {
}
