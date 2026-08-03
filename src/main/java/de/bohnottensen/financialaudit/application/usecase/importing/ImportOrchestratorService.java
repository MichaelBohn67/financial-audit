package de.bohnottensen.financialaudit.application.usecase.importing;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.BookingValidator;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImportOrchestratorService {

    private final List<TransactionSourcePort> sources;
    private final BookingRepository bookingRepository;
    private final BookingValidator bookingValidator = new BookingValidator();

    public ImportOrchestratorService(List<TransactionSourcePort> sources, BookingRepository bookingRepository) {
        this.sources = sources;
        this.bookingRepository = bookingRepository;
    }

    public ImportJobResult importFrom(Object source) {
        LocalDateTime startedAt = LocalDateTime.now();
        TransactionSourcePort transactionSource = sources.stream()
                .filter(port -> port.supports(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No import adapter supports source: " + source));

        List<Booking> imported = transactionSource.importTransactions(source);
        List<ImportValidationError> errors = new ArrayList<>();
        List<IndexedBooking> valid = new ArrayList<>();

        for (int i = 0; i < imported.size(); i++) {
            Booking booking = imported.get(i);
            List<String> bookingErrors = bookingValidator.validate(booking);
            int index = i;
            bookingErrors.forEach(error -> errors.add(new ImportValidationError(index, error)));
            if (bookingErrors.isEmpty()) {
                valid.add(new IndexedBooking(i, booking));
            }
        }

        findDuplicateForeignTransactionIds(valid, errors);
        findForeignTransactionIdGaps(valid, errors);

        List<Booking> accepted = valid.stream()
                .filter(indexedBooking -> errors.stream().noneMatch(error -> error.index() == indexedBooking.index()))
                .map(IndexedBooking::booking)
                .toList();

        bookingRepository.saveAll(accepted);
        LocalDateTime finishedAt = LocalDateTime.now();

        return new ImportJobResult(
                transactionSource.sourceType(),
                startedAt,
                finishedAt,
                accepted.size(),
                errors.size(),
                List.copyOf(errors),
                checksum(imported)
        );
    }

    private void findDuplicateForeignTransactionIds(List<IndexedBooking> bookings, List<ImportValidationError> errors) {
        Map<Long, List<IndexedBooking>> groupedByForeignId = bookings.stream()
                .filter(indexedBooking -> indexedBooking.booking().getForeignTransactionId() != null)
                .collect(Collectors.groupingBy(indexedBooking -> indexedBooking.booking().getForeignTransactionId()));

        groupedByForeignId.forEach((foreignId, indexedBookings) -> {
            if (indexedBookings.size() > 1) {
                indexedBookings.forEach(indexedBooking ->
                        errors.add(new ImportValidationError(indexedBooking.index(), "Duplicate foreign transaction id: " + foreignId)));
            }
        });
    }

    private void findForeignTransactionIdGaps(List<IndexedBooking> bookings, List<ImportValidationError> errors) {
        List<IndexedBooking> withForeignIds = bookings.stream()
                .filter(indexedBooking -> indexedBooking.booking().getForeignTransactionId() != null)
                .sorted(Comparator.comparing(indexedBooking -> indexedBooking.booking().getForeignTransactionId()))
                .toList();

        for (int i = 1; i < withForeignIds.size(); i++) {
            long previous = withForeignIds.get(i - 1).booking().getForeignTransactionId();
            long current = withForeignIds.get(i).booking().getForeignTransactionId();
            if (current - previous > 1) {
                errors.add(new ImportValidationError(withForeignIds.get(i).index(), "Gap in foreign transaction ids between " + previous + " and " + current));
            }
        }
    }

    private record IndexedBooking(int index, Booking booking) {
    }

    private String checksum(List<Booking> bookings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String content = bookings.stream()
                    .map(booking -> String.join("|",
                            String.valueOf(booking.getForeignTransactionId()),
                            String.valueOf(booking.getDescription()),
                            String.valueOf(booking.getAmount()),
                            String.valueOf(booking.getCurrency()),
                            String.valueOf(booking.getTransactionTimestamp()),
                            String.valueOf(booking.getSourceAccount()),
                            String.valueOf(booking.getDestinationAccount())
                    ))
                    .reduce("", String::concat);
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
