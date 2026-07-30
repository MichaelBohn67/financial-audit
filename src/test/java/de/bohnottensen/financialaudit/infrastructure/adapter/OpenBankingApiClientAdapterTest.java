package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenBankingApiClientAdapterTest {

    @Test
    void shouldImportTransactionsFromOpenBankingServer() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://localhost:8080/open-banking/v1/transactions?accountId=DE111"))
                .andRespond(withSuccess("""
                        [{
                          "transactionId": 42,
                          "description": "Salary",
                          "amount": 1500.00,
                          "currency": "EUR",
                          "bookedAt": "2026-07-20T10:00:00",
                          "sourceAccountId": "DE111",
                          "destinationAccountId": "DE222"
                        }]
                        """, APPLICATION_JSON));

        OpenBankingApiClientAdapter adapter = new OpenBankingApiClientAdapter(restClientBuilder, "http://localhost:8080");

        List<Booking> bookings = adapter.importTransactions("DE111");

        assertThat(bookings).hasSize(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getId()).isNull();
        assertThat(booking.getForeignTransactionId()).isEqualTo(42L);
        assertThat(booking.getDescription()).isEqualTo("Salary");
        assertThat(booking.getCurrency()).isEqualTo("EUR");
        assertThat(booking.getSourceAccount()).isEqualTo("DE111");
        assertThat(booking.getDestinationAccount()).isEqualTo("DE222");
        server.verify();
    }
}
