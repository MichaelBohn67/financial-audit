package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewActionRepository extends JpaRepository<ReviewAction, Long> {

    List<ReviewAction> findByWorkpaper(Workpaper workpaper);
}
