package de.bohnottensen.financialaudit.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingValidatorTest {

    @Test
    void shouldBeValidWhenAllFieldsCorrect() {
        Booking booking = new Booking();
        booking.setDescription("Valid");
        booking.setAmount(BigDecimal.ONE);
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(java.time.LocalDateTime.now());
        booking.setSourceAccount("DE1");
        booking.setDestinationAccount("DE2");

        BookingValidator validator = new BookingValidator();
        List<String> errors = validator.validate(booking);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReportErrorWhenAmountIsNegative() {
        Booking booking = new Booking();
        booking.setDescription("Invalid");
        booking.setAmount(new BigDecimal("-100"));
        booking.setCurrency("EUR");
        booking.setSourceAccount("DE1");
        booking.setDestinationAccount("DE2");

        BookingValidator validator = new BookingValidator();
        List<String> errors = validator.validate(booking);

        assertThat(errors).contains("Amount must be positive");
    }

    @Test
    void shouldReportErrorWhenAmountIsZero() {
        Booking booking = new Booking();
        booking.setDescription("Invalid");
        booking.setAmount(BigDecimal.ZERO);
        booking.setCurrency("EUR");
        booking.setSourceAccount("DE1");
        booking.setDestinationAccount("DE2");

        BookingValidator validator = new BookingValidator();
        List<String> errors = validator.validate(booking);

        assertThat(errors).contains("Amount must be positive");
    }

    @Test
    void shouldReportErrorWhenSourceAndDestinationAccountAreEqual() {
        Booking booking = new Booking();
        booking.setDescription("Invalid");
        booking.setAmount(BigDecimal.ONE);
        booking.setCurrency("EUR");
        booking.setTransactionTimestamp(java.time.LocalDateTime.now());
        booking.setSourceAccount("DE1");
        booking.setDestinationAccount("DE1");

        BookingValidator validator = new BookingValidator();
        List<String> errors = validator.validate(booking);

        assertThat(errors).contains("Source and Destination Account must differ");
    }

    @Test
    void shouldReportErrorWhenRequiredFieldsMissing() {
        Booking booking = new Booking();

        BookingValidator validator = new BookingValidator();
        List<String> errors = validator.validate(booking);

        assertThat(errors).contains("Description is required", "Currency is required");
    }
}
