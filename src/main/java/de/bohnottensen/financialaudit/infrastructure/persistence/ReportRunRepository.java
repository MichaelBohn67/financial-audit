package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReportRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRunRepository extends JpaRepository<ReportRun, Long> {

    List<ReportRun> findTop5ByOrderByGeneratedAtDesc();
    List<ReportRun> findTop5ByTenantIdAndProjectIdOrderByGeneratedAtDesc(String tenantId, String projectId);

    List<ReportRun> findByStatusOrderByGeneratedAtDesc(String status);
    List<ReportRun> findByTenantIdAndProjectIdAndStatusOrderByGeneratedAtDesc(String tenantId, String projectId, String status);

    List<ReportRun> findByReportNameAndTemplateVersionOrderByGeneratedAtDesc(String reportName, String templateVersion);

    List<ReportRun> findByTemplateIdOrderByGeneratedAtDesc(Long templateId);
    java.util.Optional<ReportRun> findByIdAndTenantIdAndProjectId(Long id, String tenantId, String projectId);
}
