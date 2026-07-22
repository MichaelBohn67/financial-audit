package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.AccountHolder;
import de.bohnottensen.financialaudit.domain.model.Address;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AccountHolderRepository accountHolderRepository;

    @Test
    void shouldSaveAndFindAddressHistoryForAccountHolder() {
        AccountHolder accountHolder = new AccountHolder();
        accountHolder.setFirstName("Lena");
        accountHolder.setLastName("Becker");
        accountHolder.setCustomerNumber("CUST-3001");
        accountHolder.setBirthdate(LocalDate.of(1992, 2, 12));
        accountHolder.setTin("TIN-3001");
        accountHolder.setCompany(false);
        accountHolder.setNationality("DE");
        accountHolder = accountHolderRepository.save(accountHolder);

        Address oldAddress = new Address();
        oldAddress.setAccountHolder(accountHolder);
        oldAddress.setStreet("Old Street 1");
        oldAddress.setPostalCode("10115");
        oldAddress.setCity("Berlin");
        oldAddress.setCountry("DE");
        oldAddress.setValidFrom(LocalDate.of(2020, 1, 1));
        oldAddress.setValidTo(LocalDate.of(2022, 12, 31));

        Address currentAddress = new Address();
        currentAddress.setAccountHolder(accountHolder);
        currentAddress.setStreet("New Street 5");
        currentAddress.setPostalCode("20095");
        currentAddress.setCity("Hamburg");
        currentAddress.setCountry("DE");
        currentAddress.setValidFrom(LocalDate.of(2023, 1, 1));

        Address savedOldAddress = addressRepository.save(oldAddress);
        Address savedCurrentAddress = addressRepository.save(currentAddress);

        assertThat(savedOldAddress.getId()).isNotNull();
        assertThat(savedCurrentAddress.getId()).isNotNull();
        assertThat(savedOldAddress.getAccountHolder().getId()).isEqualTo(accountHolder.getId());
        assertThat(savedCurrentAddress.getValidTo()).isNull();
    }
}
