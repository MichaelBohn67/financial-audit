package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Industriekontenrahmen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndustriekontenrahmenRepository extends JpaRepository<Industriekontenrahmen, Long> {

    Optional<Industriekontenrahmen> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Industriekontenrahmen> findByAccountClass(Integer accountClass);

    List<Industriekontenrahmen> findByAccountType(String accountType);

    List<Industriekontenrahmen> findAllByOrderByAccountNumberAsc();
}
