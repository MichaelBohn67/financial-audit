package de.bohnottensen.financialaudit.application.usecase.sampling;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRunItem;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunItemRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamplingServiceTest {

    @Test
    void shouldCreateDeterministicMusSelections() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, "100.00"),
                booking(2L, "200.00"),
                booking(3L, "300.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(55L);
            return run;
        });

        List<SamplingRunItem> firstRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            firstRunItems.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun firstRun = service.generateMusSample("task14-run-1", 999L, 3L, 1234L);

        List<SamplingRunItem> secondRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            secondRunItems.add(item);
            return item;
        });

        SamplingRun secondRun = service.generateMusSample("task14-run-2", 999L, 3L, 1234L);

        assertThat(firstRun.getSamplingStrategy()).isEqualTo("MUS");
        assertThat(firstRun.getPopulationSize()).isEqualTo(3L);
        assertThat(firstRun.getSampleSize()).isEqualTo(3L);
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(3);
        assertThat(secondRunItems).hasSize(3);
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
        assertThat(firstRunItems.stream().map(SamplingRunItem::getSelectionPoint).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getSelectionPoint).toList());
    }

    @Test
    void shouldRejectInvalidMusParameters() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);
        when(bookingRepository.findAll()).thenReturn(List.of(booking(1L, "100.00"), booking(2L, "200.00")));

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);

        assertThatThrownBy(() -> service.generateMusSample("invalid-size", 2, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleSize must be > 0");

        assertThatThrownBy(() -> service.generateMusSample("oversized", 2, 3, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oversized samples are rejected");

        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> service.generateMusSample("empty", 2, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No positive booking amounts available for MUS sampling");
    }

    @Test
    void shouldDocumentAndAllowLegitimateMultiplePointsForHighValueBooking() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);
        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, "1000.00"), booking(2L, "1.00"), booking(3L, "1.00")));
        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(99L);
            return run;
        });
        List<SamplingRunItem> items = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            items.add(item);
            return item;
        });

        SamplingRun run = new SamplingService(bookingRepository, runRepository, runItemRepository)
                .generateMusSample("high-value", 3, 3, 7);

        assertThat(items).hasSize(3);
        assertThat(items.stream().filter(item -> item.getBookingId().equals(1L)).count()).isGreaterThan(1);
        assertThat(run.getParametersJson()).contains("ALLOW_MULTIPLE_POINTS_PER_BOOKING");
        assertThat(run.getParametersJson()).contains("\"oversizedSamplePolicy\":\"REJECT\"");
    }

    @Test
    void shouldCreateDeterministicRandomSample() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, "100.00"),
                booking(2L, "200.00"),
                booking(3L, "300.00"),
                booking(4L, "400.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(56L);
            return run;
        });

        List<SamplingRunItem> firstRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            firstRunItems.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun firstRun = service.generateRandomSample("task15-random-run-1", 999L, 3L, 321L);

        List<SamplingRunItem> secondRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            secondRunItems.add(item);
            return item;
        });

        SamplingRun secondRun = service.generateRandomSample("task15-random-run-2", 999L, 3L, 321L);

        assertThat(firstRun.getSamplingStrategy()).isEqualTo("RANDOM");
        assertThat(firstRun.getSampleSize()).isEqualTo(3L);
        assertThat(firstRun.getParametersJson()).contains("\"seed\":321");
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(3);
        assertThat(secondRunItems).hasSize(3);
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
    }

    @Test
    void shouldRejectInvalidRandomParameters() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);
        when(bookingRepository.findAll()).thenReturn(List.of(booking(1L, "100.00"), booking(2L, "200.00")));

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);

        assertThatThrownBy(() -> service.generateRandomSample("test", 2, 0, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleSize must be > 0");

        assertThatThrownBy(() -> service.generateRandomSample("test", 2, 5, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleSize must be <= effective population size");
    }

    @Test
    void shouldCreateDeterministicStratifiedSample() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, "50.00"),
                booking(2L, "75.00"),
                booking(3L, "100.00"),
                booking(4L, "500.00"),
                booking(5L, "800.00"),
                booking(6L, "1200.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(57L);
            return run;
        });

        List<SamplingRunItem> firstRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            firstRunItems.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun firstRun = service.generateStratifiedSample("task15-stratified-run-1", 999L, 4L, 987L, 3);

        List<SamplingRunItem> secondRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            secondRunItems.add(item);
            return item;
        });

        SamplingRun secondRun = service.generateStratifiedSample("task15-stratified-run-2", 999L, 4L, 987L, 3);

        assertThat(firstRun.getSamplingStrategy()).isEqualTo("STRATIFIED");
        assertThat(firstRun.getSampleSize()).isEqualTo(4L);
        assertThat(firstRun.getParametersJson()).contains("\"stratumCount\":3");
        assertThat(firstRun.getParametersJson()).contains("\"seed\":987");
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(4);
        assertThat(secondRunItems).hasSize(4);
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
        assertThat(firstRunItems.stream().map(item -> item.getSelectionPoint().intValue()).distinct().toList())
                .containsAnyOf(1, 2, 3);
    }

    @Test
    void shouldRejectInvalidStratifiedParameters() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);
        when(bookingRepository.findAll()).thenReturn(List.of(booking(1L, "100.00"), booking(2L, "200.00")));

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);

        assertThatThrownBy(() -> service.generateStratifiedSample("test", 2, 0, 1L, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleSize must be > 0");

        assertThatThrownBy(() -> service.generateStratifiedSample("test", 2, 2, 1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stratumCount must be > 0");

        assertThatThrownBy(() -> service.generateStratifiedSample("test", 2, 5, 1L, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleSize must be <= effective population size");
    }

    private Booking booking(Long id, String amount) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setDescription("Sampling test booking");
        booking.setAmount(new BigDecimal(amount));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.of(2026, 8, 17, 10, 0));
        booking.setSourceAccount("SRC");
        booking.setDestinationAccount("DST");
        return booking;
    }
}
