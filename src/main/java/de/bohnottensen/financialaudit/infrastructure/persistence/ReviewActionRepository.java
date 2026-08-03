package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewActionRepository extends JpaRepository<ReviewAction, Long> {
}
