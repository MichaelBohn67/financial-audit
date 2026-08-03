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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        SamplingRun firstRun = service.generateMusSample("task14-run-1", 999L, 4L, 1234L);

        List<SamplingRunItem> secondRunItems = new ArrayList<>();
        when(runItemRepository.save(any(SamplingRunItem.class))).thenAnswer(invocation -> {
            SamplingRunItem item = invocation.getArgument(0);
            secondRunItems.add(item);
            return item;
        });

        SamplingRun secondRun = service.generateMusSample("task14-run-2", 999L, 4L, 1234L);

        assertThat(firstRun.getSamplingStrategy()).isEqualTo("MUS");
        assertThat(firstRun.getPopulationSize()).isEqualTo(3L);
        assertThat(firstRun.getSampleSize()).isEqualTo(4L);
        assertThat(secondRun.getParametersJson()).isEqualTo(firstRun.getParametersJson());

        assertThat(firstRunItems).hasSize(4);
        assertThat(secondRunItems).hasSize(4);
        assertThat(firstRunItems.stream().map(SamplingRunItem::getBookingId).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getBookingId).toList());
        assertThat(firstRunItems.stream().map(SamplingRunItem::getSelectionPoint).toList())
                .containsExactlyElementsOf(secondRunItems.stream().map(SamplingRunItem::getSelectionPoint).toList());
    }

    private Booking booking(Long id, String amount) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setForeignTransactionId(id + 1000);
        booking.setDescription("B-" + id);
        booking.setAmount(new BigDecimal(amount));
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(LocalDateTime.parse("2026-08-01T10:00:00"));
        booking.setSourceAccount("DE111");
        booking.setDestinationAccount("DE222");
        return booking;
    }
}
