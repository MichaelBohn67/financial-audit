package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class BookingWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private AuditTrailWriter auditTrailWriter;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(bookingRepository, auditTrailWriter);
    }

    @Test
    void shouldShowHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/bookings?tenantId=TENANT-1&amp;projectId=PROJECT-1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/bookings/new?tenantId=TENANT-1&amp;projectId=PROJECT-1&amp;documentId=DOC-1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/bookings?tenantId=TENANT-1&amp;projectId=PROJECT-1")));
    }

    @Test
    void shouldListBookings() throws Exception {
        Booking booking = new Booking();
        booking.setDescription("Test Booking");
        booking.setAmount(new BigDecimal("100.00"));
        booking.setCurrency("EUR");

        when(bookingRepository.findByTenantIdAndProjectId("TENANT-1", "PROJECT-1")).thenReturn(List.of(booking));

        mockMvc.perform(get("/bookings")
                .param("tenantId", "TENANT-1")
                .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-list"))
                .andExpect(model().attributeExists("bookings"));
    }

    @Test
    void shouldShowCreateForm() throws Exception {
        mockMvc.perform(get("/bookings/new")
                .param("tenantId", "TENANT-1")
                .param("projectId", "PROJECT-1")
                .param("documentId", "DOC-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-form"))
                .andExpect(model().attributeExists("booking"));
    }

    @Test
    void shouldSaveNewBooking() throws Exception {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        mockMvc.perform(post("/bookings").with(csrf())
                .param("tenantId", "TENANT-1")
                .param("projectId", "PROJECT-1")
                .param("documentId", "DOC-1")
                .param("description", "New Booking")
                .param("amount", "200.00")
                .param("currency", "EUR")
                .param("sourceAccount", "ACC1")
                .param("destinationAccount", "ACC2")
                .param("transactionTimestamp", "2023-10-27T10:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking captured = bookingCaptor.getValue();
        assertThat(captured.getTenantId()).isEqualTo("TENANT-1");
        assertThat(captured.getProjectId()).isEqualTo("PROJECT-1");
        assertThat(captured.getTransactionTimestamp()).isEqualTo(java.time.LocalDateTime.parse("2023-10-27T10:00:00"));

        verify(auditTrailWriter).record(
                eq("BOOKING"),
                eq(1L),
                eq("BOOKING_CREATED"),
                eq("WEB_USER"),
                eq("Booking created"),
                isNull(),
                contains("description=New Booking;amount=200.00;currency=EUR;timestamp=2023-10-27T10:00;source=ACC1;destination=ACC2")
        );
    }

    @Test
    void shouldSaveNewBookingWithoutTimestamp() throws Exception {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(2L);
            return b;
        });

        mockMvc.perform(post("/bookings").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1")
                        .param("description", "Auto Timestamp Booking")
                        .param("amount", "150.00")
                        .param("currency", "EUR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking captured = bookingCaptor.getValue();
        assertThat(captured.getTenantId()).isEqualTo("TENANT-1");
        assertThat(captured.getProjectId()).isEqualTo("PROJECT-1");
        assertThat(captured.getTransactionTimestamp()).isNotNull();

        ArgumentCaptor<String> auditCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter).record(
                eq("BOOKING"),
                eq(2L),
                eq("BOOKING_CREATED"),
                eq("WEB_USER"),
                eq("Booking created"),
                isNull(),
                auditCaptor.capture()
        );
        assertThat(auditCaptor.getValue()).contains("description=Auto Timestamp Booking").contains("amount=150.00");
    }

    @Test
    void shouldUpdateExistingBooking() throws Exception {
        Booking existing = new Booking();
        existing.setId(1L);
        existing.setDescription("Old Description");
        existing.setAmount(new BigDecimal("100.00"));

        when(bookingRepository.findByIdAndTenantIdAndProjectId(1L, "TENANT-1", "PROJECT-1")).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/bookings/1").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1")
                        .param("description", "Updated Description")
                        .param("amount", "200.00")
                        .param("currency", "EUR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking captured = bookingCaptor.getValue();
        assertThat(captured.getId()).isEqualTo(1L);
        assertThat(captured.getTenantId()).isEqualTo("TENANT-1");
        assertThat(captured.getProjectId()).isEqualTo("PROJECT-1");
        assertThat(captured.getTransactionTimestamp()).isNotNull();

        ArgumentCaptor<String> auditCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter).record(
                eq("BOOKING"),
                eq(1L),
                eq("BOOKING_UPDATED"),
                eq("WEB_USER"),
                eq("Booking updated"),
                any(),
                auditCaptor.capture()
        );
        assertThat(auditCaptor.getValue()).contains("description=Updated Description").contains("amount=200.00");
    }

    @Test
    void shouldShowEditForm() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setDescription("Edit Me");

        when(bookingRepository.findByIdAndTenantIdAndProjectId(1L, "TENANT-1", "PROJECT-1")).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/bookings/1/edit")
                .param("tenantId", "TENANT-1")
                .param("projectId", "PROJECT-1")
                .param("documentId", "DOC-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-form"))
                .andExpect(model().attributeExists("booking"));
    }

    @Test
    void shouldUpdateExistingBookingWithExplicitTimestampAndAllFields() throws Exception {
        Booking existing = new Booking();
        existing.setId(1L);
        existing.setDescription("Old Description");
        existing.setAmount(new BigDecimal("100.00"));
        existing.setCurrency("EUR");
        existing.setSourceAccount("SRC1");
        existing.setDestinationAccount("DST1");
        existing.setTransactionTimestamp(java.time.LocalDateTime.parse("2023-01-01T12:00:00"));

        Booking updated = new Booking();
        updated.setId(1L);
        updated.setDescription("Updated Description");
        updated.setAmount(new BigDecimal("200.00"));
        updated.setCurrency("USD");
        updated.setSourceAccount("SRC2");
        updated.setDestinationAccount("DST2");
        updated.setTransactionTimestamp(java.time.LocalDateTime.parse("2023-10-27T10:00:00"));

        when(bookingRepository.findByIdAndTenantIdAndProjectId(1L, "TENANT-1", "PROJECT-1")).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenReturn(updated);

        mockMvc.perform(post("/bookings/1").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1")
                        .param("description", "Updated Description")
                        .param("amount", "200.00")
                        .param("currency", "USD")
                        .param("sourceAccount", "SRC2")
                        .param("destinationAccount", "DST2")
                        .param("transactionTimestamp", "2023-10-27T10:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking captured = bookingCaptor.getValue();
        assertThat(captured.getId()).isEqualTo(1L);
        assertThat(captured.getTenantId()).isEqualTo("TENANT-1");
        assertThat(captured.getProjectId()).isEqualTo("PROJECT-1");
        assertThat(captured.getTransactionTimestamp()).isEqualTo(java.time.LocalDateTime.parse("2023-10-27T10:00:00"));

        ArgumentCaptor<String> previousCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> currentCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditTrailWriter).record(
                eq("BOOKING"),
                eq(1L),
                eq("BOOKING_UPDATED"),
                eq("WEB_USER"),
                eq("Booking updated"),
                previousCaptor.capture(),
                currentCaptor.capture()
        );

        assertThat(previousCaptor.getValue())
                .isEqualTo("description=Old Description;amount=100.00;currency=EUR;timestamp=2023-01-01T12:00;source=SRC1;destination=DST1");
        assertThat(currentCaptor.getValue())
                .isEqualTo("description=Updated Description;amount=200.00;currency=USD;timestamp=2023-10-27T10:00;source=SRC2;destination=DST2");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentBooking() {
        when(bookingRepository.findByIdAndTenantIdAndProjectId(99L, "TENANT-1", "PROJECT-1")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mockMvc.perform(post("/bookings/99").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1")
                        .param("description", "Does not matter")
                        .param("amount", "10.00"))
        ).hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowExceptionWhenShowingEditFormForNonExistentBooking() {
        when(bookingRepository.findByIdAndTenantIdAndProjectId(99L, "TENANT-1", "PROJECT-1")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mockMvc.perform(get("/bookings/99/edit")
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1"))
        ).hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDirectlySaveBookingAndSetScopeAndTimestamp() {
        BookingRepository repo = mock(BookingRepository.class);
        AuditTrailWriter writer = mock(AuditTrailWriter.class);
        BookingWebController webController = new BookingWebController(repo, writer);

        Booking booking = new Booking();
        booking.setDescription("Direct Test");
        when(repo.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(42L);
            return b;
        });

        String view = webController.saveBooking(booking, "T-1", "P-1", "D-1");

        assertThat(view).isEqualTo("redirect:/bookings");
        assertThat(booking.getTenantId()).isEqualTo("T-1");
        assertThat(booking.getProjectId()).isEqualTo("P-1");
        assertThat(booking.getTransactionTimestamp()).isNotNull();
        verify(repo).save(booking);
        verify(writer).record(eq("BOOKING"), eq(42L), eq("BOOKING_CREATED"), eq("WEB_USER"), eq("Booking created"), isNull(), anyString());
    }

    @Test
    void shouldDirectlyUpdateBookingAndSetIdScopeAndTimestamp() {
        BookingRepository repo = mock(BookingRepository.class);
        AuditTrailWriter writer = mock(AuditTrailWriter.class);
        BookingWebController webController = new BookingWebController(repo, writer);

        Booking existing = new Booking();
        existing.setId(99L);
        existing.setDescription("Old");
        when(repo.findByIdAndTenantIdAndProjectId(99L, "T-1", "P-1")).thenReturn(Optional.of(existing));
        when(repo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking updated = new Booking();
        updated.setDescription("New");

        String view = webController.updateBooking(99L, updated, "T-1", "P-1", "D-1");

        assertThat(view).isEqualTo("redirect:/bookings");
        assertThat(updated.getId()).isEqualTo(99L);
        assertThat(updated.getTenantId()).isEqualTo("T-1");
        assertThat(updated.getProjectId()).isEqualTo("P-1");
        assertThat(updated.getTransactionTimestamp()).isNotNull();
        verify(repo).save(updated);
        verify(writer).record(eq("BOOKING"), eq(99L), eq("BOOKING_UPDATED"), eq("WEB_USER"), eq("Booking updated"), anyString(), anyString());
    }
}
