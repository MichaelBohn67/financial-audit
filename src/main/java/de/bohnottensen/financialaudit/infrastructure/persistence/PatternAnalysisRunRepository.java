package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.PatternAnalysisRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternAnalysisRunRepository extends JpaRepository<PatternAnalysisRun, Long> {
}
