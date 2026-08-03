package de.bohnottensen.financialaudit.infrastructure.adapter;

import de.bohnottensen.financialaudit.application.ports.TransactionSourcePort;
import de.bohnottensen.financialaudit.application.usecase.importing.OpenBankingImportSource;
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
    public boolean supports(Object source) {
        return source instanceof OpenBankingImportSource || source instanceof String;
    }

    @Override
    public String sourceType() {
        return "OPEN_BANKING";
    }

    @Override
    public List<Booking> importTransactions(Object source) {
        String uri;
        if (source instanceof OpenBankingImportSource importSource) {
            StringBuilder uriBuilder = new StringBuilder("/open-banking/v1/transactions?tenantId=")
                    .append(importSource.tenantId())
                    .append("&projectId=")
                    .append(importSource.projectId());
            if (importSource.accountId() != null && !importSource.accountId().isBlank()) {
                uriBuilder.append("&accountId=").append(importSource.accountId());
            }
            uri = uriBuilder.toString();
        } else if (source instanceof String accountId) {
            uri = accountId.isBlank()
                    ? "/open-banking/v1/transactions"
                    : "/open-banking/v1/transactions?accountId=" + accountId;
        } else {
            throw new IllegalArgumentException("Source must be OpenBankingImportSource or accountId String");
        }

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
