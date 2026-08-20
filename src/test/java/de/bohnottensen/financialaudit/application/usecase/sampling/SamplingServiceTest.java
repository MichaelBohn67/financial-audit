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
        assertThat(firstRun.getRunName()).isEqualTo("task14-run-1");
        assertThat(firstRun.getSeed()).isEqualTo(1234L);
        assertThat(firstRun.getPopulationSize()).isEqualTo(3L);
        assertThat(firstRun.getSampleSize()).isEqualTo(3L);
        assertThat(firstRun.getParametersJson()).contains("\"method\":\"MUS\"");
        assertThat(firstRun.getParametersJson()).contains("\"requestedPopulationSize\":999");
        assertThat(firstRun.getParametersJson()).contains("\"effectivePopulationSize\":3");
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(3);
        assertThat(secondRunItems).hasSize(3);
        for (int i = 0; i < firstRunItems.size(); i++) {
            SamplingRunItem item = firstRunItems.get(i);
            assertThat(item.getSamplingRunId()).isEqualTo(55L);
            assertThat(item.getSampleUnitIndex()).isEqualTo(i);
            assertThat(item.getBookingId()).isNotNull();
            assertThat(item.getBookingAmount()).isNotNull();
            assertThat(item.getCumulativeAmount()).isNotNull();
            assertThat(item.getSelectionPoint()).isNotNull();
        }
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
        assertThat(firstRunItems.stream().map(SamplingRunItem::getSelectionPoint).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getSelectionPoint).toList());
    }

    @Test
    void shouldFilterNonPositiveNullAndOrderPopulationInMusSampling() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        Booking bNullId = booking(null, "100.00");
        Booking bNullAmount = booking(10L, "100.00");
        bNullAmount.setAmount(null);
        Booking bZero = booking(20L, "0.00");
        Booking bNegative = booking(30L, "-50.00");
        // reverse ordered IDs
        Booking b3 = booking(3L, "300.00");
        Booking b2 = booking(2L, "200.00");
        Booking b1 = booking(1L, "100.00");

        when(bookingRepository.findAll()).thenReturn(List.of(
                bNullId, bNullAmount, bZero, bNegative, b3, b2, b1
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(101L);
            return run;
        });

        List<SamplingRunItem> items = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            items.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun run = service.generateMusSample("filter-test", 7L, 3L, 42L);

        assertThat(run.getPopulationSize()).isEqualTo(3L);
        assertThat(items).hasSize(3);
        // Total 600.00, interval 200.00. Seed 42 randomStart ~ 145.41.
        // Cumulatives: B1 (100) -> 100 < 145.41 (not selected); B2 (200) -> 300 >= 145.41 (selected item 0); B3 (300) -> 600 >= 345.41 & 545.41 (selected items 1 and 2).
        assertThat(items.get(0).getBookingId()).isEqualTo(2L);
        assertThat(items.get(0).getSampleUnitIndex()).isEqualTo(0);
        assertThat(items.get(1).getBookingId()).isEqualTo(3L);
        assertThat(items.get(1).getSampleUnitIndex()).isEqualTo(1);
        assertThat(items.get(2).getBookingId()).isEqualTo(3L);
        assertThat(items.get(2).getSampleUnitIndex()).isEqualTo(2);
    }

    @Test
    void shouldAllowMusSampleWhenSampleSizeEqualsPopulationSize() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, "100.00"),
                booking(2L, "200.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(102L);
            return run;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun run = service.generateMusSample("exact-boundary", 2L, 2L, 10L);

        assertThat(run.getPopulationSize()).isEqualTo(2L);
        assertThat(run.getSampleSize()).isEqualTo(2L);
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
        assertThat(firstRun.getRunName()).isEqualTo("task15-random-run-1");
        assertThat(firstRun.getSeed()).isEqualTo(321L);
        assertThat(firstRun.getPopulationSize()).isEqualTo(4L);
        assertThat(firstRun.getSampleSize()).isEqualTo(3L);
        assertThat(firstRun.getParametersJson()).contains("\"seed\":321");
        assertThat(firstRun.getParametersJson()).contains("\"requestedPopulationSize\":999");
        assertThat(firstRun.getParametersJson()).contains("\"effectivePopulationSize\":4");
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(3);
        assertThat(secondRunItems).hasSize(3);
        for (int i = 0; i < firstRunItems.size(); i++) {
            SamplingRunItem item = firstRunItems.get(i);
            assertThat(item.getSamplingRunId()).isEqualTo(56L);
            assertThat(item.getSampleUnitIndex()).isEqualTo(i);
            assertThat(item.getSelectionPoint()).isEqualByComparingTo(BigDecimal.valueOf(i + 1L));
            assertThat(item.getBookingId()).isNotNull();
            assertThat(item.getBookingAmount()).isNotNull();
            assertThat(item.getCumulativeAmount()).isEqualTo(item.getBookingAmount());
        }
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
    }

    @Test
    void shouldAllowRandomSampleWhenSampleSizeEqualsPopulationSize() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, "100.00"),
                booking(2L, "200.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(60L);
            return run;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun run = service.generateRandomSample("exact-random-boundary", 2L, 2L, 42L);

        assertThat(run.getPopulationSize()).isEqualTo(2L);
        assertThat(run.getSampleSize()).isEqualTo(2L);
    }

    @Test
    void shouldFilterAndSortInLoadSamplingPopulationForRandomSample() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        Booking bNullId = booking(null, "100.00");
        Booking bNullAmount = booking(10L, "100.00");
        bNullAmount.setAmount(null);
        Booking bZero = booking(20L, "0.00");
        Booking bNegative = booking(30L, "-50.00");
        Booking b5 = booking(5L, "300.00");
        Booking b2 = booking(2L, "100.00");
        Booking b4 = booking(4L, "200.00");

        when(bookingRepository.findAll()).thenReturn(List.of(
                bNullId, bNullAmount, bZero, bNegative, b5, b2, b4
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(70L);
            return run;
        });

        List<SamplingRunItem> items = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            items.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun run = service.generateRandomSample("filter-random-test", 8L, 3L, 42L);

        assertThat(run.getPopulationSize()).isEqualTo(3L);
        assertThat(items).hasSize(3);
        // Sorted population is [2L, 4L, 5L]. With seed 42, Collections.shuffle produces [4L, 2L, 5L] (not if unsorted [5L, 2L, 4L] -> [2L, 5L, 4L]).
        assertThat(items.get(0).getBookingId()).isEqualTo(4L);
        assertThat(items.get(1).getBookingId()).isEqualTo(2L);
        assertThat(items.get(2).getBookingId()).isEqualTo(5L);
    }

    @Test
    void shouldVerifyStratumAllocationAndAssignmentMath() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        // Unsorted input
        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(5L, "50.00"),
                booking(1L, "10.00"),
                booking(4L, "40.00"),
                booking(2L, "20.00"),
                booking(3L, "30.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(71L);
            return run;
        });

        List<SamplingRunItem> items = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            items.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        SamplingRun run = service.generateStratifiedSample("strat-math-test", 5L, 4L, 999L, 3);

        assertThat(run.getPopulationSize()).isEqualTo(5L);
        assertThat(run.getSampleSize()).isEqualTo(4L);
        assertThat(items).hasSize(4);

        for (int i = 0; i < items.size(); i++) {
            assertThat(items.get(i).getSampleUnitIndex()).isEqualTo(i);
            assertThat(items.get(i).getSamplingRunId()).isEqualTo(71L);
            assertThat(items.get(i).getSelectionPoint()).isNotNull();
        }
        // Stratum 0 (size 2) gets 2 items (booking IDs 1, 2, selectionPoint=1)
        // Stratum 1 (size 2) gets 2 items (booking IDs 3, 4, selectionPoint=2)
        // Stratum 2 (size 1) gets 0 items (target=0)
        List<SamplingRunItem> stratum0Items = items.stream().filter(it -> it.getSelectionPoint().intValue() == 1).toList();
        assertThat(stratum0Items).hasSize(2);
        for (SamplingRunItem it : stratum0Items) {
            assertThat(it.getBookingId()).isIn(1L, 2L);
        }
        List<SamplingRunItem> stratum1Items = items.stream().filter(it -> it.getSelectionPoint().intValue() == 2).toList();
        assertThat(stratum1Items).hasSize(2);
        for (SamplingRunItem it : stratum1Items) {
            assertThat(it.getBookingId()).isIn(3L, 4L);
        }
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
        assertThat(firstRun.getRunName()).isEqualTo("task15-stratified-run-1");
        assertThat(firstRun.getSeed()).isEqualTo(987L);
        assertThat(firstRun.getPopulationSize()).isEqualTo(6L);
        assertThat(firstRun.getSampleSize()).isEqualTo(4L);
        assertThat(firstRun.getParametersJson()).contains("\"stratumCount\":3");
        assertThat(firstRun.getParametersJson()).contains("\"seed\":987");
        assertThat(firstRun.getParametersJson()).contains("\"requestedPopulationSize\":999");
        assertThat(firstRun.getParametersJson()).contains("\"effectivePopulationSize\":6");
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(4);
        assertThat(secondRunItems).hasSize(4);
        for (int i = 0; i < firstRunItems.size(); i++) {
            SamplingRunItem item = firstRunItems.get(i);
            assertThat(item.getSamplingRunId()).isEqualTo(57L);
            assertThat(item.getSampleUnitIndex()).isEqualTo(i);
            assertThat(item.getBookingId()).isNotNull();
            assertThat(item.getBookingAmount()).isNotNull();
            assertThat(item.getCumulativeAmount()).isEqualTo(item.getBookingAmount());
            assertThat(item.getSelectionPoint()).isNotNull();
        }
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
        assertThat(firstRunItems.stream().map(item -> item.getSelectionPoint().intValue()).distinct().toList())
                .containsAnyOf(1, 2, 3);
    }

    @Test
    void shouldSortByAmountAndIdAndHandleStratifiedBoundaries() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SamplingRunRepository runRepository = mock(SamplingRunRepository.class);
        SamplingRunItemRepository runItemRepository = mock(SamplingRunItemRepository.class);

        // Unsorted bookings with some equal amounts
        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(6L, "100.00"),
                booking(2L, "50.00"),
                booking(1L, "50.00"),
                booking(5L, "100.00"),
                booking(4L, "75.00")
        ));

        when(runRepository.save(any(SamplingRun.class))).thenAnswer(invocation -> {
            SamplingRun run = invocation.getArgument(0);
            run.setId(58L);
            return run;
        });

        List<SamplingRunItem> items = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            items.add(item);
            return item;
        });

        SamplingService service = new SamplingService(bookingRepository, runRepository, runItemRepository);
        // sampleSize == populationSize (5 items)
        SamplingRun run = service.generateStratifiedSample("stratified-boundary", 5L, 5L, 123L, 2);

        assertThat(run.getPopulationSize()).isEqualTo(5L);
        assertThat(run.getSampleSize()).isEqualTo(5L);
        assertThat(items).hasSize(5);

        for (int i = 0; i < items.size(); i++) {
            assertThat(items.get(i).getSampleUnitIndex()).isEqualTo(i);
            assertThat(items.get(i).getSamplingRunId()).isEqualTo(58L);
        }
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
