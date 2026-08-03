package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.sampling.SamplingService;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRunItem;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunItemRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: SamplingService with real H2 DB — MUS, random, and stratified
 * sampling strategies produce reproducible, correctly sized samples with persisted items.
 */
@SpringBootTest
@Transactional
class SamplingIntegrationTest {

    @Autowired
    private SamplingService samplingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SamplingRunRepository samplingRunRepository;

    @Autowired
    private SamplingRunItemRepository samplingRunItemRepository;

    @Test
    void shouldGenerateMusSampleWithCorrectSizeAndPersistItems() {
        seedBookings(10);

        SamplingRun run = samplingService.generateMusSample("MUS-Q1", 10, 3, 42L);

        assertThat(run.getId()).isNotNull();
        assertThat(run.getSamplingStrategy()).isEqualTo("MUS");
        assertThat(run.getSeed()).isEqualTo(42L);
        assertThat(run.getSampleSize()).isEqualTo(3L);
        assertThat(run.getParametersJson()).contains("\"method\":\"MUS\"");

        assertThat(samplingRunRepository.findById(run.getId())).isPresent();
        List<SamplingRunItem> items = samplingRunItemRepository.findAll();
        assertThat(items).hasSize(3);
    }

    @Test
    void shouldProduceReproducibleMusSampleForSameSeed() {
        seedBookings(10);

        SamplingRun run1 = samplingService.generateMusSample("MUS-R1", 10, 3, 99L);
        // Flush and reload to check persistence
        samplingRunRepository.flush();

        SamplingRun run2 = samplingService.generateMusSample("MUS-R2", 10, 3, 99L);

        // Same seed → same booking IDs selected (order-deterministic)
        List<SamplingRunItem> items1 = samplingRunItemRepository.findAll().stream()
                .filter(i -> i.getSamplingRunId().equals(run1.getId()))
                .sorted(java.util.Comparator.comparing(SamplingRunItem::getBookingId))
                .toList();
        List<SamplingRunItem> items2 = samplingRunItemRepository.findAll().stream()
                .filter(i -> i.getSamplingRunId().equals(run2.getId()))
                .sorted(java.util.Comparator.comparing(SamplingRunItem::getBookingId))
                .toList();

        assertThat(items1.stream().map(SamplingRunItem::getBookingId).toList())
                .isEqualTo(items2.stream().map(SamplingRunItem::getBookingId).toList());
    }

    @Test
    void shouldGenerateRandomSampleWithCorrectSize() {
        seedBookings(10);

        SamplingRun run = samplingService.generateRandomSample("RAND-Q1", 10, 4, 77L);

        assertThat(run.getSamplingStrategy()).isEqualTo("RANDOM");
        assertThat(run.getSampleSize()).isEqualTo(4L);

        long itemCount = samplingRunItemRepository.findAll().stream()
                .filter(i -> i.getSamplingRunId().equals(run.getId()))
                .count();
        assertThat(itemCount).isEqualTo(4);
    }

    @Test
    void shouldGenerateStratifiedSampleWithCorrectSizeAcrossStrata() {
        seedBookings(9);

        SamplingRun run = samplingService.generateStratifiedSample("STRAT-Q1", 9, 3, 55L, 3);

        assertThat(run.getSamplingStrategy()).isEqualTo("STRATIFIED");
        assertThat(run.getSampleSize()).isEqualTo(3L);

        long itemCount = samplingRunItemRepository.findAll().stream()
                .filter(i -> i.getSamplingRunId().equals(run.getId()))
                .count();
        assertThat(itemCount).isEqualTo(3);
    }

    private void seedBookings(int count) {
        for (int i = 1; i <= count; i++) {
            Booking b = new Booking();
            b.setDescription("Booking " + i);
            b.setAmount(new BigDecimal(i * 100));
            b.setCurrency("EUR");
            b.setTransactionTimestamp(LocalDateTime.parse("2026-05-01T10:00:00").plusDays(i));
            b.setSourceAccount("DE11000000001234567890");
            b.setDestinationAccount("DE22000000009876543210");
            bookingRepository.save(b);
        }
        bookingRepository.flush();
    }
}
