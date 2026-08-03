package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.BenfordDigitStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenfordDigitStatRepository extends JpaRepository<BenfordDigitStat, Long> {
}
