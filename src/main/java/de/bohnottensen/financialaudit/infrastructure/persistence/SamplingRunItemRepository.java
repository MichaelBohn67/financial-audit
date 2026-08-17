package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.SamplingRunItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SamplingRunItemRepository extends JpaRepository<SamplingRunItem, Long> {
    List<SamplingRunItem> findBySamplingRunIdOrderBySampleUnitIndex(Long samplingRunId);
}
