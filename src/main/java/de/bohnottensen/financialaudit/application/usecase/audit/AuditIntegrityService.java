package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class AuditIntegrityService {
    private final AuditEventRepository events;
    private final AuditEventHasher hasher;
    public AuditIntegrityService(AuditEventRepository events, AuditEventHasher hasher) {
        this.events = events;
        this.hasher = hasher;
    }

    public String latestHash() {
        return events.findTopByOrderByMetadata_OccurredAtDesc().map(AuditEvent::getIntegrityHash)
                .filter(Objects::nonNull).orElse("");
    }

    public String calculateHash(AuditEvent event, String previousHash) {
        return hasher.calculateHash(event, previousHash);
    }

    public boolean verify(AuditEvent event, String expectedPreviousHash) {
        boolean independentAutomaticRecord = event.getPreviousHash() == null || event.getPreviousHash().isEmpty();
        String expected = independentAutomaticRecord ? "" : expectedPreviousHash;
        return Objects.equals(expected, event.getPreviousHash())
                && Objects.equals(hasher.calculateHash(event, expected), event.getIntegrityHash());
    }
}
