package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.application.usecase.importing.OpenBankingImportSource;
import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenBankingApiClientAdapterTest {

    @Test
    void shouldCheckSupportsAndSourceType() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        assertThat(adapter.supports(new OpenBankingImportSource("T1", "P1", "A1"))).isTrue();
        assertThat(adapter.supports("account-id")).isTrue();
        assertThat(adapter.supports(123)).isFalse();
        assertThat(adapter.supports(null)).isFalse();
        assertThat(adapter.sourceType()).isEqualTo("OPEN_BANKING");
    }

    @Test
    void shouldThrowWhenSourceIsInvalid() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        assertThatThrownBy(() -> adapter.importTransactions(123L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source must be OpenBankingImportSource or accountId String");

        assertThatThrownBy(() -> adapter.importTransactions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source must be OpenBankingImportSource or accountId String");
    }

    @Test
    void shouldImportTransactionsFromOpenBankingServerWithAccountIdString() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions?accountId=DE111"))
                .andRespond(withSuccess("""
                        [{
                          "transactionId": 42,
                          "description": "  Salary  ",
                          "amount": 1500.00,
                          "currency": "  eur  ",
                          "bookedAt": "2026-07-20T10:00:00",
                          "sourceAccountId": "  de111  ",
                          "destinationAccountId": "  de222  "
                        }]
                        """, APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions("DE111");

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getId()).isNull();
        assertThat(booking.getForeignTransactionId()).isEqualTo(42L);
        assertThat(booking.getDescription()).isEqualTo("Salary");
        assertThat(booking.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(booking.getCurrency()).isEqualTo("EUR");
        assertThat(booking.getTransactionTimestamp()).isEqualTo(LocalDateTime.of(2026, 7, 20, 10, 0, 0));
        assertThat(booking.getSourceAccount()).isEqualTo("DE111");
        assertThat(booking.getDestinationAccount()).isEqualTo("DE222");
        server.verify();
    }

    @Test
    void shouldImportTransactionsWithStringBlankAccountId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions"))
                .andRespond(withSuccess("[]", APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions("   ");

        assertThat(bookings).isEmpty();
        server.verify();
    }

    @Test
    void shouldImportTransactionsWithOpenBankingImportSourceAndAccountId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions?tenantId=TEN1&projectId=PROJ1&accountId=ACC1"))
                .andRespond(withSuccess("""
                        [{
                          "transactionId": 99,
                          "description": null,
                          "amount": 250.50,
                          "currency": null,
                          "bookedAt": "2026-07-21T11:00:00",
                          "sourceAccountId": null,
                          "destinationAccountId": null
                        }]
                        """, APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions(new OpenBankingImportSource("TEN1", "PROJ1", "ACC1"));

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getForeignTransactionId()).isEqualTo(99L);
        assertThat(booking.getDescription()).isNull();
        assertThat(booking.getAmount()).isEqualByComparingTo("250.50");
        assertThat(booking.getCurrency()).isNull();
        assertThat(booking.getTransactionTimestamp()).isEqualTo(LocalDateTime.of(2026, 7, 21, 11, 0, 0));
        assertThat(booking.getSourceAccount()).isNull();
        assertThat(booking.getDestinationAccount()).isNull();
        server.verify();
    }

    @Test
    void shouldImportTransactionsWithOpenBankingImportSourceWithoutAccountId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions?tenantId=TEN1&projectId=PROJ1"))
                .andRespond(withSuccess("[]", APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions(new OpenBankingImportSource("TEN1", "PROJ1", null));

        assertThat(bookings).isEmpty();
        server.verify();
    }

    @Test
    void shouldImportTransactionsWithOpenBankingImportSourceWithBlankAccountId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions?tenantId=TEN1&projectId=PROJ1"))
                .andRespond(withSuccess("[]", APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions(new OpenBankingImportSource("TEN1", "PROJ1", "  "));

        assertThat(bookings).isEmpty();
        server.verify();
    }

    @Test
    void shouldHandleNullBodyResponse() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions?accountId=DE111"))
                .andRespond(withSuccess("", APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions("DE111");

        assertThat(bookings).isEmpty();
        server.verify();
    }
}
