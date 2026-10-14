package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Canonical serialization and hashing of an {@link AuditEvent}.
 *
 * This is the single definition of the audit trail's integrity hash. Both the
 * {@code AuditTrailWriter} and the Hibernate {@code AutomaticAuditEventListener} must use
 * it, otherwise stored hashes cannot be reproduced during verification.
 *
 * Deliberately free of any repository/EntityManager dependency so that it can be used from
 * inside a Hibernate event listener. Injecting the repository-backed
 * {@link AuditIntegrityService} there would create the cycle
 * entityManagerFactory -> listener -> repository -> entityManagerFactory.
 */
@Component
public class AuditEventHasher {

    private static final String FIELD_SEPARATOR = "|";

    /**
     * Recompute the integrity hash for an event.
     *
     * @param event         the event to hash
     * @param previousHash  hash of the preceding record, empty for an unlinked record
     */
    public String calculateHash(AuditEvent event, String previousHash) {
        String canonical = String.join(FIELD_SEPARATOR,
                nullToEmpty(previousHash),
                nullToEmpty(event.getEntityType()),
                String.valueOf(event.getEntityId()),
                nullToEmpty(event.getEventType()),
                nullToEmpty(event.getActor()),
                nullToEmpty(event.getSummary()),
                nullToEmpty(event.getPreviousValue()),
                nullToEmpty(event.getCurrentValue()),
                String.valueOf(event.getOccurredAt()),
                nullToEmpty(event.getRequestId()),
                nullToEmpty(event.getSourceIp()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate audit integrity hash", e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
