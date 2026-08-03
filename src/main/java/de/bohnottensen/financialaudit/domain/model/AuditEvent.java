package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Embedded
    private AuditEventMetadata metadata = new AuditEventMetadata();

    @PrePersist
    protected void onCreate() {
        if (metadata == null) {
            metadata = new AuditEventMetadata();
        }
        metadata.initializeOccurredAtIfMissing();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActor() {
        return metadata.getActor();
    }

    public void setActor(String actor) {
        metadata.setActor(actor);
    }

    public String getSummary() {
        return metadata.getSummary();
    }

    public void setSummary(String summary) {
        metadata.setSummary(summary);
    }

    public String getPreviousValue() {
        return metadata.getPreviousValue();
    }

    public void setPreviousValue(String previousValue) {
        metadata.setPreviousValue(previousValue);
    }

    public String getCurrentValue() {
        return metadata.getCurrentValue();
    }

    public void setCurrentValue(String currentValue) {
        metadata.setCurrentValue(currentValue);
    }

    public LocalDateTime getOccurredAt() {
        return metadata.getOccurredAt();
    }

    public AuditEventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AuditEventMetadata metadata) {
        this.metadata = metadata == null ? new AuditEventMetadata() : metadata;
    }
}
