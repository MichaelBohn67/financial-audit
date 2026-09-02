package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReportArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportArchiveRepository extends JpaRepository<ReportArchive, Long> {
    Optional<ReportArchive> findByReportRunIdAndTenantIdAndProjectId(Long reportRunId, String tenantId, String projectId);
}
