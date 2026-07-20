package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void shouldSaveAndFindBooking() {
        Booking booking = new Booking();
        booking.setDescription("Test Transaction");
        booking.setAmount(new BigDecimal("100.00"));
        booking.setCurrency("EUR");
        booking.setSourceAccount("DE1234567890");
        booking.setDestinationAccount("DE0987654321");
        booking.setTransactionTimestamp(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        assertThat(savedBooking.getId()).isNotNull();
        assertThat(bookingRepository.findById(savedBooking.getId())).isPresent();
    }
}
