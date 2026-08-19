package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.*;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DomainModelApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private AccountHolderRepository accountHolderRepository;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private AddressRepository addressRepository;

    @MockBean
    private FindingRepository findingRepository;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldReturnBookingsForAuthorizedAuditor() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setDescription("Test Booking");
        booking.setAmount(new BigDecimal("100.50"));
        booking.setCurrency("EUR");
        booking.setSourceAccount("DE111");
        booking.setDestinationAccount("DE222");
        booking.setTransactionTimestamp(LocalDateTime.now());

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings")
                        .param("tenantId", "TENANT_1")
                        .param("projectId", "PROJECT_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Test Booking"))
                .andExpect(jsonPath("$[0].amount").value(100.50))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].sourceAccount").value("DE111"))
                .andExpect(jsonPath("$[0].destinationAccount").value("DE222"));
    }

    @Test
    @WithMockUser(username = "lead", roles = "LEAD_AUDITOR")
    void shouldReturnAccountHoldersForLeadAuditor() throws Exception {
        AccountHolder holder = new AccountHolder();
        holder.setId(10L);
        holder.setFirstName("Max");
        holder.setLastName("Mustermann");
        holder.setCustomerNumber("CUST-100");
        holder.setBirthdate(LocalDate.of(1985, 5, 20));
        holder.setTin("DE123456789");
        holder.setCompany(false);
        holder.setNationality("DE");

        when(accountHolderRepository.findAll()).thenReturn(List.of(holder));

        mockMvc.perform(get("/api/account-holders")
                        .param("tenantId", "TENANT_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].firstName").value("Max"))
                .andExpect(jsonPath("$[0].lastName").value("Mustermann"))
                .andExpect(jsonPath("$[0].customerNumber").value("CUST-100"))
                .andExpect(jsonPath("$[0].company").value(false))
                .andExpect(jsonPath("$[0].nationality").value("DE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldReturnAccountsForAdmin() throws Exception {
        AccountHolder holder = new AccountHolder();
        holder.setId(5L);

        Account account = new Account();
        account.setId(20L);
        account.setIban("DE99999999999999999999");
        account.setCurrency("EUR");
        account.setAccountHolder(holder);

        when(accountRepository.findAll()).thenReturn(List.of(account));

        mockMvc.perform(get("/api/accounts")
                        .param("tenantId", "TENANT_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].iban").value("DE99999999999999999999"))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].accountHolderId").value(5));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldReturnAddressesForAdmin() throws Exception {
        AccountHolder holder = new AccountHolder();
        holder.setId(5L);

        Address address = new Address();
        address.setId(30L);
        address.setAccountHolder(holder);
        address.setStreet("Hauptstrasse 1");
        address.setPostalCode("10115");
        address.setCity("Berlin");
        address.setCountry("DE");
        address.setValidFrom(LocalDate.of(2020, 1, 1));
        address.setValidTo(LocalDate.of(2030, 12, 31));

        when(addressRepository.findAll()).thenReturn(List.of(address));

        mockMvc.perform(get("/api/addresses")
                        .param("tenantId", "TENANT_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].street").value("Hauptstrasse 1"))
                .andExpect(jsonPath("$[0].postalCode").value("10115"))
                .andExpect(jsonPath("$[0].city").value("Berlin"))
                .andExpect(jsonPath("$[0].country").value("DE"))
                .andExpect(jsonPath("$[0].accountHolderId").value(5));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldReturnFindingsForAuditor() throws Exception {
        Booking booking = new Booking();
        booking.setId(7L);

        Finding finding = new Finding();
        finding.setId(40L);
        finding.setBooking(booking);
        finding.setRuleName("AML_THRESHOLD");
        finding.setAlertDescription("Amount too high");
        finding.setRiskLevel("HIGH");
        finding.setStatus("NEW");
        finding.setAnalysisRunId("RUN-1");
        finding.setRuleVersion("v1.0");
        finding.setRunContext("AUDIT-2026");
        finding.setAuditorComment("Follow up required");

        when(findingRepository.findAll()).thenReturn(List.of(finding));

        mockMvc.perform(get("/api/findings")
                        .param("tenantId", "TENANT_1")
                        .param("projectId", "PROJECT_1")
                        .param("documentId", "DOC_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(40))
                .andExpect(jsonPath("$[0].bookingId").value(7))
                .andExpect(jsonPath("$[0].ruleName").value("AML_THRESHOLD"))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].auditorComment").value("Follow up required"));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldDenyAuditorFromTenantLevelAccess() throws Exception {
        mockMvc.perform(get("/api/account-holders")
                        .param("tenantId", "TENANT_1"))
                .andExpect(status().isForbidden());
    }
}
