package de.bohnottensen.financialaudit.application.usecase.materiality;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.*;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class MaterialityService {
    private final MaterialityConfigRepository configs;
    private final BookingRepository bookings;
    private final FindingRepository findings;
    private final AuditTrailWriter audit;

    public MaterialityService(MaterialityConfigRepository configs, BookingRepository bookings,
                               FindingRepository findings, AuditTrailWriter audit) {
        this.configs = configs; this.bookings = bookings; this.findings = findings; this.audit = audit;
    }

    @Transactional
    public MaterialityConfig save(String name, BigDecimal overall, BigDecimal performance, BigDecimal deMinimis) {
        if (name == null || name.isBlank() || overall == null || performance == null || deMinimis == null
                || overall.signum() <= 0 || performance.signum() <= 0 || deMinimis.signum() < 0
                || performance.compareTo(overall) > 0 || deMinimis.compareTo(performance) > 0)
            throw new IllegalArgumentException("Materiality thresholds must satisfy 0 <= de minimis <= performance <= overall");
        configs.findAll().forEach(c -> { c.setActive(false); configs.save(c); });
        MaterialityConfig c = new MaterialityConfig(); c.setName(name); c.setPlanningMateriality(overall);
        c.setPerformanceMateriality(performance); c.setDeMinimisThreshold(deMinimis); c.setActive(true);
        MaterialityConfig saved = configs.save(c);
        audit.record("MaterialityConfig", saved.getId(), "CREATE", "system", "Materiality configured", null, name);
        return saved;
    }
    public MaterialityConfig active() { return configs.findFirstByActiveTrueOrderByCreatedAtDesc().orElseThrow(() -> new IllegalStateException("No active materiality configuration")); }
    public MaterialityResult evaluate(Booking booking) {
        MaterialityConfig c = active(); BigDecimal amount = booking.getAmount().abs();
        String classification = amount.compareTo(c.getPlanningMateriality()) >= 0 ? "OVERALL" :
                amount.compareTo(c.getPerformanceMateriality()) >= 0 ? "PERFORMANCE" :
                amount.compareTo(c.getDeMinimisThreshold()) >= 0 ? "DE_MINIMIS" : "BELOW_THRESHOLD";
        return new MaterialityResult(booking.getId(), amount, classification, !classification.equals("BELOW_THRESHOLD"));
    }
    @Transactional
    public List<MaterialityResult> evaluateAll(String account) {
        return bookings.findBySourceAccountOrDestinationAccount(account, account).stream().map(this::evaluate).toList();
    }
    public record MaterialityResult(Long bookingId, BigDecimal amount, String classification, boolean material) {}
}
