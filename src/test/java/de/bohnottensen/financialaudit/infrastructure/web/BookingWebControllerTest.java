package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingRepository bookingRepository;

    @Test
    void shouldListBookings() throws Exception {
        Booking booking = new Booking();
        booking.setDescription("Test Booking");
        booking.setAmount(new BigDecimal("100.00"));
        booking.setCurrency("EUR");

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-list"))
                .andExpect(model().attributeExists("bookings"));
    }

    @Test
    void shouldShowCreateForm() throws Exception {
        mockMvc.perform(get("/bookings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-form"))
                .andExpect(model().attributeExists("booking"));
    }

    @Test
    void shouldSaveNewBooking() throws Exception {
        mockMvc.perform(post("/bookings")
                .param("description", "New Booking")
                .param("amount", "200.00")
                .param("currency", "EUR")
                .param("sourceAccount", "ACC1")
                .param("destinationAccount", "ACC2")
                .param("transactionTimestamp", "2023-10-27T10:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));
    }

    @Test
    void shouldShowEditForm() throws Exception {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setDescription("Edit Me");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/bookings/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking-form"))
                .andExpect(model().attributeExists("booking"));
    }
}
