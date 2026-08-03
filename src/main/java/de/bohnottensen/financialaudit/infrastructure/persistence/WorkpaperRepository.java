package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Workpaper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkpaperRepository extends JpaRepository<Workpaper, Long> {
}
