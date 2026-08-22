package de.bohnottensen.financialaudit.application.usecase.importing;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.BookingValidator;
import de.bohnottensen.financialaudit.domain.model.ImportJob;
import de.bohnottensen.financialaudit.domain.model.ImportJobProtocolEntry;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobProtocolEntryRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final ImportJobRepository importJobRepository;
    private final ImportJobProtocolEntryRepository protocolEntryRepository;
    private final DigestFactory digestFactory;
    private final BookingValidator bookingValidator = new BookingValidator();

    @Autowired
    public ImportOrchestratorService(List<TransactionSourcePort> sources,
                                     BookingRepository bookingRepository,
                                     ImportJobRepository importJobRepository,
                                     ImportJobProtocolEntryRepository protocolEntryRepository) {
        this(sources, bookingRepository, importJobRepository, protocolEntryRepository,
                () -> MessageDigest.getInstance("SHA-256"));
    }

    ImportOrchestratorService(List<TransactionSourcePort> sources,
                              BookingRepository bookingRepository,
                              ImportJobRepository importJobRepository,
                              ImportJobProtocolEntryRepository protocolEntryRepository,
                              DigestFactory digestFactory) {
        this.sources = sources;
        this.bookingRepository = bookingRepository;
        this.importJobRepository = importJobRepository;
        this.protocolEntryRepository = protocolEntryRepository;
        this.digestFactory = digestFactory;
    }

    public ImportJobResult importFrom(Object source) {
        return importFrom(new ImportRunContext("LEGACY", "LEGACY", "LEGACY", "legacy"), source);
    }

    public ImportJobResult importFrom(ImportRunContext runContext, Object source) {
        LocalDateTime startedAt = LocalDateTime.now();
        ImportJob importJob = new ImportJob();
        importJob.setSourceType("UNKNOWN");
        importJob.setStatus("RUNNING");
        importJob.setRunContext(runContext.asProtocolContext());
        importJob.setTenantId(runContext.tenantId());
        importJob.setProjectId(runContext.projectId());
        importJob.setDocumentId(runContext.documentId());
        importJob.setStartedAt(startedAt);
        importJob.setRecordCount(0);
        importJob.setImportedCount(0);
        importJob.setInvalidCount(0);
        importJob = importJobRepository.save(importJob);

        try {
        TransactionSourcePort transactionSource = sources.stream()
                .filter(port -> port.supports(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No import adapter supports source: " + source));
        importJob.setSourceType(transactionSource.sourceType());
        importJob = importJobRepository.save(importJob);

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

        if ("CSV".equalsIgnoreCase(transactionSource.sourceType())) {
            applyCsvSpecificValidations(valid, errors);
        } else if ("OPEN_BANKING".equalsIgnoreCase(transactionSource.sourceType())) {
            applyOpenBankingSpecificValidations(valid, errors);
        }

        List<Booking> accepted = valid.stream()
                .filter(indexedBooking -> errors.stream().noneMatch(error -> error.index() == indexedBooking.index()))
                .map(IndexedBooking::booking)
                .toList();

        bookingRepository.saveAll(accepted);
        LocalDateTime finishedAt = LocalDateTime.now();
        String checksum = checksum(imported);

        importJob.setStatus("COMPLETED");
        importJob.setFinishedAt(finishedAt);
        importJob.setRecordCount(imported.size());
        importJob.setImportedCount(accepted.size());
        importJob.setInvalidCount(errors.size());
        importJob.setChecksum(checksum);
        importJobRepository.save(importJob);
        persistValidationProtocol(importJob.getId(), errors);

        return new ImportJobResult(
                importJob.getId(),
                importJob.getStatus(),
                importJob.getRunContext(),
                transactionSource.sourceType(),
                startedAt,
                finishedAt,
                accepted.size(),
                errors.size(),
                List.copyOf(errors),
                checksum
        );
        } catch (RuntimeException ex) {
            LocalDateTime finishedAt = LocalDateTime.now();
            importJob.setStatus("FAILED");
            importJob.setFinishedAt(finishedAt);
            importJob.setErrorMessage(ex.getMessage());
            importJobRepository.save(importJob);
            persistProtocolEntry(importJob.getId(), 0, "ERROR", ex.getMessage() == null ? "Import failed" : ex.getMessage());
            throw ex;
        }
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

    private void applyCsvSpecificValidations(List<IndexedBooking> bookings, List<ImportValidationError> errors) {
        for (IndexedBooking indexedBooking : bookings) {
            if (indexedBooking.booking().getForeignTransactionId() == null) {
                errors.add(new ImportValidationError(indexedBooking.index(), "Document number (foreign transaction id) is required for CSV import"));
            }
        }
        findDuplicateForeignTransactionIds(bookings, errors);
        findForeignTransactionIdGaps(bookings, errors);
    }

    private void applyOpenBankingSpecificValidations(List<IndexedBooking> bookings, List<ImportValidationError> errors) {
        LocalDateTime now = LocalDateTime.now();
        for (IndexedBooking indexedBooking : bookings) {
            Booking booking = indexedBooking.booking();
            if (booking.getForeignTransactionId() == null || booking.getForeignTransactionId() <= 0) {
                errors.add(new ImportValidationError(indexedBooking.index(), "OpenBanking transaction id is required and must be positive"));
            }
            if (booking.getTransactionTimestamp() != null && booking.getTransactionTimestamp().isAfter(now.plusMinutes(5))) {
                errors.add(new ImportValidationError(indexedBooking.index(), "OpenBanking booking timestamp is in the future"));
            }
            if (!isOpenBankingAccountIdValid(booking.getSourceAccount())) {
                errors.add(new ImportValidationError(indexedBooking.index(), "OpenBanking source account id format is invalid"));
            }
            if (!isOpenBankingAccountIdValid(booking.getDestinationAccount())) {
                errors.add(new ImportValidationError(indexedBooking.index(), "OpenBanking destination account id format is invalid"));
            }
        }
        findDuplicateForeignTransactionIds(bookings, errors);
        findDuplicateBusinessTransactions(bookings, errors);
    }

    private boolean isOpenBankingAccountIdValid(String accountId) {
        return accountId != null
                && accountId.matches("[A-Z]{2}[A-Z0-9]{3,32}");
    }

    private void findDuplicateBusinessTransactions(List<IndexedBooking> bookings, List<ImportValidationError> errors) {
        Map<String, List<IndexedBooking>> grouped = bookings.stream()
                .collect(Collectors.groupingBy(this::businessTransactionKey));

        grouped.forEach((key, groupedBookings) -> {
            if (groupedBookings.size() > 1) {
                groupedBookings.forEach(indexedBooking ->
                        errors.add(new ImportValidationError(indexedBooking.index(), "Duplicate OpenBanking transaction payload: " + key)));
            }
        });
    }

    private String businessTransactionKey(IndexedBooking indexedBooking) {
        Booking booking = indexedBooking.booking();
        return String.join("|",
                String.valueOf(booking.getTransactionTimestamp()),
                String.valueOf(booking.getAmount()),
                String.valueOf(booking.getCurrency()),
                String.valueOf(booking.getSourceAccount()),
                String.valueOf(booking.getDestinationAccount()));
    }

    private void persistValidationProtocol(Long jobId, List<ImportValidationError> errors) {
        for (int i = 0; i < errors.size(); i++) {
            ImportValidationError error = errors.get(i);
            persistProtocolEntry(jobId, i, "VALIDATION", "index=" + error.index() + ";error=" + error.error());
        }
    }

    private void persistProtocolEntry(Long jobId, int entryIndex, String level, String message) {
        ImportJobProtocolEntry protocolEntry = new ImportJobProtocolEntry();
        protocolEntry.setImportJobId(jobId);
        protocolEntry.setEntryIndex(entryIndex);
        protocolEntry.setLevel(level);
        protocolEntry.setMessage(message);
        protocolEntryRepository.save(protocolEntry);
    }

    private String checksum(List<Booking> bookings) {
        try {
            MessageDigest digest = digestFactory.create();
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

    @FunctionalInterface
    interface DigestFactory {
        MessageDigest create() throws NoSuchAlgorithmException;
    }
}
