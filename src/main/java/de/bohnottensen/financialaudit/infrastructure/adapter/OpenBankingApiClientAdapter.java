package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.domain.model.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class OpenBankingApiClientAdapter implements TransactionSourcePort {

    private final RestClient restClient;

    public OpenBankingApiClientAdapter(RestClient.Builder restClientBuilder,
                                       @Value("${openbanking.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public List<Booking> importTransactions(Object source) {
        String accountId = source instanceof String ? (String) source : null;
        String uri = accountId == null || accountId.isBlank()
                ? "/open-banking/v1/transactions"
                : "/open-banking/v1/transactions?accountId=" + accountId;

        OpenBankingTransactionResponse[] response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenBankingTransactionResponse[].class);

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .map(this::toBooking)
                .toList();
    }

    private Booking toBooking(OpenBankingTransactionResponse response) {
        Booking booking = new Booking();
        booking.setForeignTransactionId(response.transactionId());
        booking.setDescription(response.description());
        booking.setAmount(response.amount());
        booking.setCurrency(response.currency());
        booking.setTransactionTimestamp(response.bookedAt());
        booking.setSourceAccount(response.sourceAccountId());
        booking.setDestinationAccount(response.destinationAccountId());
        return booking;
    }

    record OpenBankingTransactionResponse(Long transactionId, String description, java.math.BigDecimal amount,
                                          String currency, LocalDateTime bookedAt, String sourceAccountId,
                                          String destinationAccountId) {
    }
}
