package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class AuditIntegrityService {
    private final AuditEventRepository events;
    public AuditIntegrityService(AuditEventRepository events) { this.events = events; }

    public String latestHash() {
        return events.findTopByOrderByMetadata_OccurredAtDesc().map(AuditEvent::getIntegrityHash)
                .filter(java.util.Objects::nonNull).orElse("");
    }

    public String calculateHash(AuditEvent event, String previousHash) {
        String canonical = String.join("|", nullToEmpty(previousHash), nullToEmpty(event.getEntityType()),
                String.valueOf(event.getEntityId()), nullToEmpty(event.getEventType()), nullToEmpty(event.getActor()),
                nullToEmpty(event.getSummary()), nullToEmpty(event.getPreviousValue()), nullToEmpty(event.getCurrentValue()),
                String.valueOf(event.getOccurredAt()), nullToEmpty(event.getRequestId()), nullToEmpty(event.getSourceIp()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to calculate audit integrity hash", e); }
    }

    public boolean verify(AuditEvent event, String expectedPreviousHash) {
        boolean independentAutomaticRecord = event.getPreviousHash() == null || event.getPreviousHash().isEmpty();
        String expected = independentAutomaticRecord ? "" : expectedPreviousHash;
        return java.util.Objects.equals(expected, event.getPreviousHash())
                && java.util.Objects.equals(calculateHash(event, expected), event.getIntegrityHash());
    }
    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
