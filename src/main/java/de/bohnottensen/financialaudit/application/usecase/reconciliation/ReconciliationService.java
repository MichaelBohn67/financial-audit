package de.bohnottensen.financialaudit.application.usecase.reconciliation;

import de.bohnottensen.financialaudit.domain.model.Account;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.ReconciliationResult;
import de.bohnottensen.financialaudit.infrastructure.persistence.AccountRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final BookingRepository bookingRepository;

    public ReconciliationService(AccountRepository accountRepository, BookingRepository bookingRepository) {
        this.accountRepository = accountRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationResult reconcileAccount(String iban) {
        Optional<Account> accountOpt = accountRepository.findAll().stream()
                .filter(a -> a.getIban().equals(iban))
                .findFirst();

        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for IBAN: " + iban);
        }

        Account account = accountOpt.get();
        List<Booking> bookings = bookingRepository.findBySourceAccountOrDestinationAccount(iban, iban);

        BigDecimal internalSum = bookings.stream()
                .map(b -> {
                    if (iban.equals(b.getDestinationAccount())) {
                        return b.getAmount();
                    } else {
                        return b.getAmount().negate();
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discrepancy = account.getBalance().subtract(internalSum);
        boolean isBalanced = discrepancy.compareTo(BigDecimal.ZERO) == 0;
        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();

        return new ReconciliationResult(
                iban,
                internalSum,
                account.getBalance(),
                discrepancy,
                isBalanced,
                bookingIds
        );
    }
}
