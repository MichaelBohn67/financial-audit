package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditIntegrityService;
import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit-trail")
@PreAuthorize("hasAnyRole('LEAD_AUDITOR','ADMIN')")
public class AuditTrailApiController {
    private final AuditEventRepository events;
    private final AuditIntegrityService integrity;
    public AuditTrailApiController(AuditEventRepository events, AuditIntegrityService integrity) { this.events = events; this.integrity = integrity; }

    @GetMapping
    public List<AuditEvent> events() { return events.findTop10ByOrderByMetadata_OccurredAtDesc(); }

    @GetMapping("/verify")
    public Verification verify() {
        List<AuditEvent> ordered = events.findAll().stream()
                .sorted(java.util.Comparator.comparing(AuditEvent::getOccurredAt)).toList();
        String previous = "";
        for (AuditEvent event : ordered) {
            if (!integrity.verify(event, previous)) return new Verification(false, event.getId());
            previous = event.getIntegrityHash();
        }
        return new Verification(true, null);
    }
    public record Verification(boolean valid, Long firstInvalidEventId) {}
}
