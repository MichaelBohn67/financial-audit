package de.bohnottensen.financialaudit.application.usecase.reconciliation;

import de.bohnottensen.financialaudit.domain.model.Account;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.ReconciliationResult;
import de.bohnottensen.financialaudit.infrastructure.persistence.AccountRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReconciliationServiceTest {

    private AccountRepository accountRepository;
    private BookingRepository bookingRepository;
    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountRepository.class);
        bookingRepository = Mockito.mock(BookingRepository.class);
        reconciliationService = new ReconciliationService(accountRepository, bookingRepository);
    }

    @Test
    void shouldReconcileBalancedAccount() {
        String iban = "DE12345678";
        Account account = new Account();
        account.setIban(iban);
        account.setBalance(new BigDecimal("100.00"));

        Booking b1 = new Booking();
        b1.setId(1L);
        b1.setAmount(new BigDecimal("150.00"));
        b1.setDestinationAccount(iban); // Credit

        Booking b2 = new Booking();
        b2.setId(2L);
        b2.setAmount(new BigDecimal("50.00"));
        b2.setSourceAccount(iban); // Debit

        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(bookingRepository.findBySourceAccountOrDestinationAccount(iban, iban))
                .thenReturn(List.of(b1, b2));

        ReconciliationResult result = reconciliationService.reconcileAccount(iban);

        assertThat(result.iban()).isEqualTo(iban);
        assertThat(result.internalSum()).isEqualByComparingTo("100.00");
        assertThat(result.externalBalance()).isEqualByComparingTo("100.00");
        assertThat(result.discrepancy()).isEqualByComparingTo("0.00");
        assertThat(result.isBalanced()).isTrue();
    }

    @Test
    void shouldDetectDiscrepancy() {
        String iban = "DE12345678";
        Account account = new Account();
        account.setIban(iban);
        account.setBalance(new BigDecimal("200.00")); // External says 200

        Booking b1 = new Booking();
        b1.setId(1L);
        b1.setAmount(new BigDecimal("150.00"));
        b1.setDestinationAccount(iban); // Internal sum is 150

        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(bookingRepository.findBySourceAccountOrDestinationAccount(iban, iban))
                .thenReturn(List.of(b1));

        ReconciliationResult result = reconciliationService.reconcileAccount(iban);

        assertThat(result.internalSum()).isEqualByComparingTo("150.00");
        assertThat(result.externalBalance()).isEqualByComparingTo("200.00");
        assertThat(result.discrepancy()).isEqualByComparingTo("50.00");
        assertThat(result.isBalanced()).isFalse();
    }

    @Test
    void shouldFindExactAccountAmongMultipleAccounts() {
        String targetIban = "DE12345678";
        Account otherAccount = new Account();
        otherAccount.setIban("DE99999999");
        otherAccount.setBalance(new BigDecimal("500.00"));

        Account targetAccount = new Account();
        targetAccount.setIban(targetIban);
        targetAccount.setBalance(new BigDecimal("300.00"));

        when(accountRepository.findAll()).thenReturn(List.of(otherAccount, targetAccount));
        when(bookingRepository.findBySourceAccountOrDestinationAccount(targetIban, targetIban))
                .thenReturn(List.of());

        ReconciliationResult result = reconciliationService.reconcileAccount(targetIban);

        assertThat(result.iban()).isEqualTo(targetIban);
        assertThat(result.externalBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findAll()).thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reconciliationService.reconcileAccount("UNKNOWN_IBAN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found for IBAN");
    }
}
