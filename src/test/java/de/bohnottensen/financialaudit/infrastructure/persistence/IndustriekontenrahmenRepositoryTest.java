package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Industriekontenrahmen;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IndustriekontenrahmenRepositoryTest {

    @Autowired
    private IndustriekontenrahmenRepository repository;

    @Test
    void testSaveAndFindByAccountNumber() {
        Industriekontenrahmen ikr = new Industriekontenrahmen(
                "0840", "Fuhrpark", 0, "08 BGA", "AKTIV", "Lkw, Pkw"
        );
        Industriekontenrahmen saved = repository.save(ikr);
        assertThat(saved.getId()).isNotNull();

        Optional<Industriekontenrahmen> found = repository.findByAccountNumber("0840");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Fuhrpark");
        assertThat(found.get().getAccountClass()).isEqualTo(0);
        assertThat(found.get().getAccountType()).isEqualTo("AKTIV");
        assertThat(repository.existsByAccountNumber("0840")).isTrue();
        assertThat(repository.existsByAccountNumber("9999")).isFalse();
    }

    @Test
    void testFindByAccountClassAndType() {
        repository.save(new Industriekontenrahmen("2800", "Bank", 2, "28 Flüssige Mittel", "AKTIV", "Bankguthaben"));
        repository.save(new Industriekontenrahmen("2880", "Kasse", 2, "28 Flüssige Mittel", "AKTIV", "Kassenbestand"));
        repository.save(new Industriekontenrahmen("4400", "Verbindlichkeiten LuL", 4, "44 Verbindlichkeiten", "PASSIV", "Lieferanten"));

        List<Industriekontenrahmen> class2 = repository.findByAccountClass(2);
        assertThat(class2).hasSize(2);

        List<Industriekontenrahmen> passiv = repository.findByAccountType("PASSIV");
        assertThat(passiv).hasSize(1);
        assertThat(passiv.get(0).getAccountNumber()).isEqualTo("4400");
    }
}
