package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReportRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRunRepository extends JpaRepository<ReportRun, Long> {

    List<ReportRun> findTop5ByOrderByGeneratedAtDesc();

    List<ReportRun> findByStatusOrderByGeneratedAtDesc(String status);

    List<ReportRun> findByReportNameAndTemplateVersionOrderByGeneratedAtDesc(String reportName, String templateVersion);

    List<ReportRun> findByTemplateIdOrderByGeneratedAtDesc(Long templateId);
}
