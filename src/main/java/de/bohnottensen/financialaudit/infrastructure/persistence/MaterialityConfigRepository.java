package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.MaterialityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MaterialityConfigRepository extends JpaRepository<MaterialityConfig, Long> {
    Optional<MaterialityConfig> findFirstByActiveTrueOrderByCreatedAtDesc();
}
