package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Account;
import de.bohnottensen.financialaudit.domain.model.AccountHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountHolderRepository accountHolderRepository;

    @Test
    void shouldSaveAndFindAccountWithAccountHolder() {
        AccountHolder accountHolder = new AccountHolder();
        accountHolder.setFirstName("Erika");
        accountHolder.setLastName("Musterfrau");
        accountHolder.setCustomerNumber("CUST-2001");
        accountHolder.setBirthdate(LocalDate.of(1985, 9, 14));
        accountHolder.setTin("TIN-2001");
        accountHolder.setCompany(false);
        accountHolder = accountHolderRepository.save(accountHolder);

        Account account = new Account();
        account.setIban("DE89370400440532013000");
        account.setCurrency("EUR");
        account.setAccountHolder(accountHolder);

        Account savedAccount = accountRepository.save(account);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(accountRepository.findById(savedAccount.getId())).isPresent();
        assertThat(savedAccount.getAccountHolder().getId()).isEqualTo(accountHolder.getId());
    }
}
