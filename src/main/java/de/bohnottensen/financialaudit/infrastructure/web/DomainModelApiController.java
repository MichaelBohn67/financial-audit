package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.*;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DomainModelApiController {

    private final BookingRepository bookingRepository;
    private final AccountHolderRepository accountHolderRepository;
    private final AccountRepository accountRepository;
    private final AddressRepository addressRepository;
    private final FindingRepository findingRepository;

    public DomainModelApiController(BookingRepository bookingRepository,
                                    AccountHolderRepository accountHolderRepository,
                                    AccountRepository accountRepository,
                                    AddressRepository addressRepository,
                                    FindingRepository findingRepository) {
        this.bookingRepository = bookingRepository;
        this.accountHolderRepository = accountHolderRepository;
        this.accountRepository = accountRepository;
        this.addressRepository = addressRepository;
        this.findingRepository = findingRepository;
    }

    @GetMapping("/bookings")
    public List<BookingView> bookings() {
        return bookingRepository.findAll().stream()
                .map(this::toBookingView)
                .toList();
    }

    @GetMapping("/account-holders")
    public List<AccountHolderView> accountHolders() {
        return accountHolderRepository.findAll().stream()
                .map(this::toAccountHolderView)
                .toList();
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts() {
        return accountRepository.findAll().stream()
                .map(this::toAccountView)
                .toList();
    }

    @GetMapping("/addresses")
    public List<AddressView> addresses() {
        return addressRepository.findAll().stream()
                .map(this::toAddressView)
                .toList();
    }

    @GetMapping("/findings")
    public List<FindingView> findings() {
        return findingRepository.findAll().stream()
                .map(this::toFindingView)
                .toList();
    }

    private BookingView toBookingView(Booking booking) {
        return new BookingView(
                booking.getId(),
                booking.getDescription(),
                booking.getAmount(),
                booking.getCurrency(),
                booking.getTransactionTimestamp(),
                booking.getSourceAccount(),
                booking.getDestinationAccount(),
                booking.getCreatedAt()
        );
    }

    private AccountHolderView toAccountHolderView(AccountHolder accountHolder) {
        return new AccountHolderView(
                accountHolder.getId(),
                accountHolder.getFirstName(),
                accountHolder.getLastName(),
                accountHolder.getCustomerNumber(),
                accountHolder.getBirthdate(),
                accountHolder.getTin(),
                accountHolder.isCompany(),
                accountHolder.getNationality(),
                accountHolder.getCreatedAt()
        );
    }

    private AccountView toAccountView(Account account) {
        return new AccountView(
                account.getId(),
                account.getIban(),
                account.getCurrency(),
                account.getAccountHolder() != null ? account.getAccountHolder().getId() : null,
                account.getCreatedAt()
        );
    }

    private AddressView toAddressView(Address address) {
        return new AddressView(
                address.getId(),
                address.getAccountHolder() != null ? address.getAccountHolder().getId() : null,
                address.getStreet(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountry(),
                address.getValidFrom(),
                address.getValidTo()
        );
    }

    private FindingView toFindingView(Finding finding) {
        return new FindingView(
                finding.getId(),
                finding.getBooking() != null ? finding.getBooking().getId() : null,
                finding.getRuleName(),
                finding.getAlertDescription(),
                finding.getRiskLevel(),
                finding.getStatus(),
                finding.getAuditorComment(),
                finding.getCreatedAt()
        );
    }

    public record BookingView(Long id, String description, java.math.BigDecimal amount, String currency,
                              LocalDateTime transactionTimestamp, String sourceAccount, String destinationAccount,
                              LocalDateTime createdAt) {
    }

    public record AccountHolderView(Long id, String firstName, String lastName, String customerNumber,
                                    LocalDate birthdate, String tin, boolean company, String nationality,
                                    LocalDateTime createdAt) {
    }

    public record AccountView(Long id, String iban, String currency, Long accountHolderId,
                              LocalDateTime createdAt) {
    }

    public record AddressView(Long id, Long accountHolderId, String street, String postalCode, String city,
                              String country, LocalDate validFrom, LocalDate validTo) {
    }

    public record FindingView(Long id, Long bookingId, String ruleName, String alertDescription, String riskLevel,
                              String status, String auditorComment, LocalDateTime createdAt) {
    }
}
