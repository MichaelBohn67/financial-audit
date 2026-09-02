package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditTrailWriter {

    private final AuditEventRepository auditEventRepository;
    private final AuditIntegrityService integrity;

    public AuditTrailWriter(AuditEventRepository auditEventRepository, AuditIntegrityService integrity) {
        this.auditEventRepository = auditEventRepository;
        this.integrity = integrity;
    }

    public AuditEvent record(String entityType,
                             Long entityId,
                             String eventType,
                             String actor,
                             String summary,
                             String previousValue,
                             String currentValue) {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEntityType(entityType);
        auditEvent.setEntityId(entityId);
        auditEvent.setEventType(eventType);
        auditEvent.setActor(actor);
        auditEvent.setSummary(summary);
        auditEvent.setPreviousValue(previousValue);
        auditEvent.setCurrentValue(currentValue);
        auditEvent.getMetadata().initializeOccurredAtIfMissing();
        auditEvent.setRequestId(requestId());
        auditEvent.setSourceIp(org.slf4j.MDC.get("sourceIp"));
        auditEvent.setPreviousHash(integrity.latestHash());
        auditEvent.setIntegrityHash(integrity.calculateHash(auditEvent, auditEvent.getPreviousHash()));
        return auditEventRepository.save(auditEvent);
    }

    private String requestId() {
        return org.slf4j.MDC.get("requestId");
    }
}
