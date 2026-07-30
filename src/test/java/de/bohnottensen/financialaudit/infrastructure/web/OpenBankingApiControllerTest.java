package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.Account;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.AccountRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenBankingApiController.class)
class OpenBankingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @Test
    void shouldListAccounts() throws Exception {
        Account account = new Account();
        account.setIban("DE111");
        account.setCurrency("EUR");

        when(accountRepository.findAll()).thenReturn(List.of(account));

        mockMvc.perform(get("/open-banking/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value("DE111"))
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }

    @Test
    void shouldFilterTransactionsByAccountId() throws Exception {
        Booking matching = new Booking();
        matching.setId(1L);
        matching.setDescription("Salary");
        matching.setAmount(new BigDecimal("1000.00"));
        matching.setCurrency("EUR");
        matching.setTransactionTimestamp(LocalDateTime.parse("2026-07-20T10:00:00"));
        matching.setSourceAccount("DE111");
        matching.setDestinationAccount("DE222");

        Booking other = new Booking();
        other.setId(2L);
        other.setDescription("Other");
        other.setAmount(new BigDecimal("50.00"));
        other.setCurrency("EUR");
        other.setTransactionTimestamp(LocalDateTime.parse("2026-07-20T10:00:00"));
        other.setSourceAccount("DE333");
        other.setDestinationAccount("DE444");

        when(bookingRepository.findAll()).thenReturn(List.of(matching, other));

        mockMvc.perform(get("/open-banking/v1/transactions").param("accountId", "DE111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(1))
                .andExpect(jsonPath("$[0].sourceAccountId").value("DE111"))
                .andExpect(jsonPath("$[0].destinationAccountId").value("DE222"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
