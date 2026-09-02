package de.bohnottensen.financialaudit.infrastructure.persistence;
import de.bohnottensen.financialaudit.domain.model.AuditProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditProcedureRepository extends JpaRepository<AuditProcedure, Long> {
    List<AuditProcedure> findByEngagementId(Long engagementId);
}
