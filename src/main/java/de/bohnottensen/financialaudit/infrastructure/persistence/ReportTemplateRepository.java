package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    Optional<ReportTemplate> findByNameAndVersion(String name, String version);

    List<ReportTemplate> findByNameOrderByCreatedAtDesc(String name);

    Optional<ReportTemplate> findFirstByNameAndActiveTrueOrderByCreatedAtDesc(String name);
}
