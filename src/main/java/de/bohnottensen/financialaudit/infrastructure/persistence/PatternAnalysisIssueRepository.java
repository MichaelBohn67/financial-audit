package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.PatternAnalysisIssue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternAnalysisIssueRepository extends JpaRepository<PatternAnalysisIssue, Long> {
}
