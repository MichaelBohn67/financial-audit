package de.bohnottensen.financialaudit.infrastructure.persistence;
import de.bohnottensen.financialaudit.domain.model.WorkpaperEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WorkpaperEvidenceRepository extends JpaRepository<WorkpaperEvidence, Long> {
    List<WorkpaperEvidence> findByWorkpaperId(Long workpaperId);
}
