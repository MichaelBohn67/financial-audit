package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/bookings")
public class BookingWebController {

    private final BookingRepository bookingRepository;

    public BookingWebController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public String listBookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "booking-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("booking", new Booking());
        return "booking-form";
    }

    @PostMapping
    public String saveBooking(@ModelAttribute Booking booking) {
        if (booking.getTransactionTimestamp() == null) {
            booking.setTransactionTimestamp(java.time.LocalDateTime.now());
        }
        bookingRepository.save(booking);
        return "redirect:/bookings";
    }

    @PostMapping("/{id}")
    public String updateBooking(@PathVariable Long id, @ModelAttribute Booking booking) {
        booking.setId(id);
        if (booking.getTransactionTimestamp() == null) {
            booking.setTransactionTimestamp(java.time.LocalDateTime.now());
        }
        bookingRepository.save(booking);
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid booking Id:" + id));
        model.addAttribute("booking", booking);
        return "booking-form";
    }
}
