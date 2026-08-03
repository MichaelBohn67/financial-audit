package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/bookings")
public class BookingWebController {

    private final BookingRepository bookingRepository;
    private final AuditTrailWriter auditTrailWriter;

    public BookingWebController(BookingRepository bookingRepository, AuditTrailWriter auditTrailWriter) {
        this.bookingRepository = bookingRepository;
        this.auditTrailWriter = auditTrailWriter;
    }

    @GetMapping
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public String listBookings(Model model,
                               @RequestParam String tenantId,
                               @RequestParam String projectId) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "booking-list";
    }

    @GetMapping("/new")
    @PreAuthorize("@scopeAccessPolicy.canAccessDocument(authentication, #tenantId, #projectId, #documentId)")
    public String showCreateForm(Model model,
                                 @RequestParam String tenantId,
                                 @RequestParam String projectId,
                                 @RequestParam String documentId) {
        model.addAttribute("booking", new Booking());
        return "booking-form";
    }

    @PostMapping
    @PreAuthorize("@scopeAccessPolicy.canAccessDocument(authentication, #tenantId, #projectId, #documentId)")
    public String saveBooking(@ModelAttribute Booking booking,
                              @RequestParam String tenantId,
                              @RequestParam String projectId,
                              @RequestParam String documentId) {
        if (booking.getTransactionTimestamp() == null) {
            booking.setTransactionTimestamp(java.time.LocalDateTime.now());
        }
        Booking savedBooking = bookingRepository.save(booking);
        auditTrailWriter.record(
                "BOOKING",
                savedBooking.getId(),
                "BOOKING_CREATED",
                "WEB_USER",
                "Booking created",
                null,
                bookingSnapshot(savedBooking)
        );
        return "redirect:/bookings";
    }

    @PostMapping("/{id}")
    @PreAuthorize("@scopeAccessPolicy.canAccessDocument(authentication, #tenantId, #projectId, #documentId)")
    public String updateBooking(@PathVariable Long id,
                                @ModelAttribute Booking booking,
                                @RequestParam String tenantId,
                                @RequestParam String projectId,
                                @RequestParam String documentId) {
        Booking previous = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid booking Id:" + id));
        String previousValue = bookingSnapshot(previous);
        booking.setId(id);
        if (booking.getTransactionTimestamp() == null) {
            booking.setTransactionTimestamp(java.time.LocalDateTime.now());
        }
        Booking savedBooking = bookingRepository.save(booking);
        auditTrailWriter.record(
                "BOOKING",
                savedBooking.getId(),
                "BOOKING_UPDATED",
                "WEB_USER",
                "Booking updated",
                previousValue,
                bookingSnapshot(savedBooking)
        );
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("@scopeAccessPolicy.canAccessDocument(authentication, #tenantId, #projectId, #documentId)")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               @RequestParam String tenantId,
                               @RequestParam String projectId,
                               @RequestParam String documentId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid booking Id:" + id));
        model.addAttribute("booking", booking);
        return "booking-form";
    }

    private String bookingSnapshot(Booking booking) {
        return "description=" + booking.getDescription()
                + ";amount=" + booking.getAmount()
                + ";currency=" + booking.getCurrency()
                + ";timestamp=" + booking.getTransactionTimestamp()
                + ";source=" + booking.getSourceAccount()
                + ";destination=" + booking.getDestinationAccount();
    }
}
