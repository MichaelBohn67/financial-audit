package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.AccountHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountHolderRepositoryTest {

    @Autowired
    private AccountHolderRepository accountHolderRepository;

    @Test
    void shouldSaveAndFindAccountHolder() {
        AccountHolder accountHolder = new AccountHolder();
        accountHolder.setFirstName("Max");
        accountHolder.setLastName("Mustermann");
        accountHolder.setCustomerNumber("CUST-1001");
        accountHolder.setBirthdate(LocalDate.of(1990, 5, 20));
        accountHolder.setTin("TIN-1001");
        accountHolder.setCompany(false);

        AccountHolder savedAccountHolder = accountHolderRepository.save(accountHolder);

        assertThat(savedAccountHolder.getId()).isNotNull();
        assertThat(accountHolderRepository.findById(savedAccountHolder.getId())).isPresent();
    }
}
