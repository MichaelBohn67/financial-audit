package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.Account;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.AccountRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/open-banking/v1")
public class OpenBankingApiController {

    private final AccountRepository accountRepository;
    private final BookingRepository bookingRepository;

    public OpenBankingApiController(AccountRepository accountRepository, BookingRepository bookingRepository) {
        this.accountRepository = accountRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/accounts")
    @PreAuthorize("@scopeAccessPolicy.canAccessTenant(authentication, #tenantId)")
    public List<OpenBankingAccountView> accounts(@RequestParam String tenantId) {
        return accountRepository.findAll().stream()
                .map(this::toAccountView)
                .toList();
    }

    @GetMapping("/transactions")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public List<OpenBankingTransactionView> transactions(@RequestParam String tenantId,
                                                         @RequestParam String projectId,
                                                         @RequestParam(required = false) String accountId) {
        return bookingRepository.findAll().stream()
                .filter(booking -> accountId == null
                        || accountId.equals(booking.getSourceAccount())
                        || accountId.equals(booking.getDestinationAccount()))
                .map(this::toTransactionView)
                .toList();
    }

    private OpenBankingAccountView toAccountView(Account account) {
        return new OpenBankingAccountView(
                account.getIban(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }

    private OpenBankingTransactionView toTransactionView(Booking booking) {
        return new OpenBankingTransactionView(
                booking.getId(),
                booking.getDescription(),
                booking.getAmount(),
                booking.getCurrency(),
                booking.getTransactionTimestamp(),
                booking.getSourceAccount(),
                booking.getDestinationAccount()
        );
    }

    public record OpenBankingAccountView(String accountId, String currency, LocalDateTime createdAt) {
    }

    public record OpenBankingTransactionView(Long transactionId, String description, BigDecimal amount, String currency,
                                             LocalDateTime bookedAt, String sourceAccountId, String destinationAccountId) {
    }
}
