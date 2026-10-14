package de.bohnottensen.financialaudit.infrastructure.initialization;

import de.bohnottensen.financialaudit.domain.model.Industriekontenrahmen;
import de.bohnottensen.financialaudit.infrastructure.persistence.IndustriekontenrahmenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IndustriekontenrahmenInitializerTest {

    @Autowired
    private IndustriekontenrahmenRepository repository;

    @Autowired
    private IndustriekontenrahmenInitializer initializer;

    @Test
    void shouldInitializeAndPersistAllIkrAccounts() {
        // When
        List<Industriekontenrahmen> result = initializer.initializeIndustriekontenrahmen();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(repository.count()).isGreaterThanOrEqualTo(50);

        // Verify key accounts in different classes
        Optional<Industriekontenrahmen> kasse = repository.findByAccountNumber("2880");
        assertThat(kasse).isPresent();
        assertThat(kasse.get().getName()).isEqualTo("Kasse");
        assertThat(kasse.get().getAccountClass()).isEqualTo(2);
        assertThat(kasse.get().getAccountType()).isEqualTo("AKTIV");

        Optional<Industriekontenrahmen> bank = repository.findByAccountNumber("2800");
        assertThat(bank).isPresent();
        assertThat(bank.get().getName()).contains("Guthaben bei Kreditinstituten");

        Optional<Industriekontenrahmen> lulVerbindlichkeiten = repository.findByAccountNumber("4400");
        assertThat(lulVerbindlichkeiten).isPresent();
        assertThat(lulVerbindlichkeiten.get().getAccountClass()).isEqualTo(4);
        assertThat(lulVerbindlichkeiten.get().getAccountType()).isEqualTo("PASSIV");

        Optional<Industriekontenrahmen> umsatzerloese = repository.findByAccountNumber("5000");
        assertThat(umsatzerloese).isPresent();
        assertThat(umsatzerloese.get().getAccountClass()).isEqualTo(5);
        assertThat(umsatzerloese.get().getAccountType()).isEqualTo("ERTRAG");

        Optional<Industriekontenrahmen> rohstoffe = repository.findByAccountNumber("6000");
        assertThat(rohstoffe).isPresent();
        assertThat(rohstoffe.get().getAccountClass()).isEqualTo(6);
        assertThat(rohstoffe.get().getAccountType()).isEqualTo("AUFWAND");

        Optional<Industriekontenrahmen> ebk = repository.findByAccountNumber("8000");
        assertThat(ebk).isPresent();
        assertThat(ebk.get().getAccountClass()).isEqualTo(8);
        assertThat(ebk.get().getAccountType()).isEqualTo("ABSCHLUSS");

        Optional<Industriekontenrahmen> klr = repository.findByAccountNumber("9000");
        assertThat(klr).isPresent();
        assertThat(klr.get().getAccountClass()).isEqualTo(9);
        assertThat(klr.get().getAccountType()).isEqualTo("KLR");
    }

    @Test
    void shouldBeIdempotentWhenExecutedMultipleTimes() {
        long initialCount = repository.count();
        List<Industriekontenrahmen> firstRun = initializer.initializeIndustriekontenrahmen();
        long countAfterFirstRun = repository.count();

        List<Industriekontenrahmen> secondRun = initializer.addIndustriekontenrahmen();
        long countAfterSecondRun = repository.count();

        assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun);
        assertThat(secondRun).hasSize(firstRun.size());
    }

    @Test
    void shouldVerifyAllKontenklassen0to9Exist() {
        initializer.initializeIndustriekontenrahmen();

        for (int k = 0; k <= 9; k++) {
            List<Industriekontenrahmen> accountsInClass = repository.findByAccountClass(k);
            assertThat(accountsInClass)
                    .as("Kontenklasse %d must have accounts", k)
                    .isNotEmpty();
        }
    }

    @Test
    void shouldFindAllOrderedByAccountNumber() {
        initializer.initializeIndustriekontenrahmen();
        List<Industriekontenrahmen> orderedList = repository.findAllByOrderByAccountNumberAsc();

        assertThat(orderedList).isNotEmpty();
        for (int i = 0; i < orderedList.size() - 1; i++) {
            assertThat(orderedList.get(i).getAccountNumber().compareTo(orderedList.get(i + 1).getAccountNumber()))
                    .isLessThanOrEqualTo(0);
        }
    }
}
