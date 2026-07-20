package de.bohnottensen.financialaudit.infrastructure.persistence;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FindingRepositoryTest {

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void shouldSaveAndFindFinding() {
        Booking booking = new Booking();
        booking.setDescription("Source for finding");
        booking.setAmount(new BigDecimal("100.00"));
        booking.setCurrency("EUR");
        booking.setSourceAccount("S1");
        booking.setDestinationAccount("D1");
        booking.setTransactionTimestamp(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        Finding finding = new Finding();
        finding.setBooking(booking);
        finding.setRuleName("HighAmountRule");
        finding.setAlertDescription("High amount");
        finding.setRiskLevel("HIGH");

        Finding savedFinding = findingRepository.save(finding);

        assertThat(savedFinding.getId()).isNotNull();
        assertThat(findingRepository.findById(savedFinding.getId())).isPresent();
        assertThat(savedFinding.getBooking().getId()).isEqualTo(booking.getId());
    }
}
