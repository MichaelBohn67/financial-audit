package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import jakarta.persistence.Entity;
import org.hibernate.Session;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class AutomaticAuditEventListener implements PostInsertEventListener, PostUpdateEventListener,
        PostDeleteEventListener {
    private static final Set<String> AUDITED_ENTITIES = new HashSet<>(Set.of(
            "Booking", "Finding", "Workpaper", "ReviewAction", "SamplingRun", "ReportRun",
            "MaterialityConfig", "ImportJob", "AuditEngagement", "AuditRisk", "AuditProcedure", "WorkpaperEvidence",
            "Tenant", "AuditProject", "ProjectMembership"));

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (isAudited(event.getEntity())) {
            write(event.getSession(), event.getEntity(), event.getId(), "CREATE", null,
                    snapshot(event.getPersister(), event.getState()));
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (isAudited(event.getEntity())) {
            write(event.getSession(), event.getEntity(), event.getId(), "UPDATE",
                    snapshot(event.getPersister(), event.getOldState()),
                    snapshot(event.getPersister(), event.getState()));
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (isAudited(event.getEntity())) {
            write(event.getSession(), event.getEntity(), event.getId(), "DELETE",
                    snapshot(event.getPersister(), event.getDeletedState()), null);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private boolean isAudited(Object entity) {
        return entity != null && !(entity instanceof AuditEvent)
                && AUDITED_ENTITIES.contains(entity.getClass().getSimpleName());
    }

    private void write(Session session, Object entity, Object id, String operation,
                       String previous, String current) {
        if (id == null) {
            return;
        }
        AuditEvent event = new AuditEvent();
        event.setEntityType(entity.getClass().getSimpleName());
        event.setEntityId(((Number) id).longValue());
        setScope(event, entity);
        event.setEventType("PERSISTENCE_" + operation);
        event.setActor(actor());
        event.setSummary("Automatic persistence audit event");
        event.setPreviousValue(previous);
        event.setCurrentValue(current);
        event.getMetadata().initializeOccurredAtIfMissing();
        // Never query or flush through the active Hibernate session here. A post event
        // runs during flush and doing so causes recursive flushes and assertion failures.
        // Automatic events form independently verifiable append records; manual events
        // may additionally link to the preceding record through AuditTrailWriter.
        String previousHash = "";
        event.setPreviousHash(previousHash);
        event.setRequestId(org.slf4j.MDC.get("requestId"));
        event.setSourceIp(org.slf4j.MDC.get("sourceIp"));
        event.setIntegrityHash(hash(event, previousHash));
        session.persist(event);
    }

    private void setScope(AuditEvent event, Object entity) {
        String tenant = invokeString(entity, "getTenantId");
        String project = invokeString(entity, "getProjectId");
        if (project == null) project = invokeString(entity, "getScopeProjectId");
        if (tenant == null && entity instanceof de.bohnottensen.financialaudit.domain.model.Finding finding
                && finding.getBooking() != null) {
            tenant = finding.getBooking().getTenantId();
            project = finding.getBooking().getProjectId();
        }
        event.setTenantId(tenant);
        event.setProjectId(project);
    }

    private String invokeString(Object entity, String methodName) {
        try {
            Method method = entity.getClass().getMethod(methodName);
            Object value = method.invoke(entity);
            return value instanceof String string && !string.isBlank() ? string : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String hash(AuditEvent event, String previousHash) {
        try {
            String value = String.join("|", previousHash, event.getEntityType(), String.valueOf(event.getEntityId()),
                    event.getEventType(), event.getActor(), event.getSummary(), String.valueOf(event.getPreviousValue()),
                    String.valueOf(event.getCurrentValue()), String.valueOf(event.getOccurredAt()),
                    event.getRequestId() == null ? "" : event.getRequestId(),
                    event.getSourceIp() == null ? "" : event.getSourceIp());
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to calculate audit integrity hash", e); }
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String snapshot(EntityPersister persister, Object[] state) {
        if (state == null) {
            return null;
        }
        StringBuilder snapshot = new StringBuilder();
        String[] names = persister.getPropertyNames();
        for (int i = 0; i < names.length && i < state.length; i++) {
            if (isSensitive(names[i])) {
                snapshot.append(names[i]).append("=[REDACTED];");
            } else {
                snapshot.append(names[i]).append('=').append(value(state[i])).append(';');
            }
        }
        return snapshot.length() > 4000 ? snapshot.substring(0, 4000) : snapshot.toString();
    }

    private boolean isSensitive(String name) {
        String normalized = name.toLowerCase();
        return normalized.contains("password") || normalized.contains("secret")
                || normalized.contains("credential") || normalized.contains("token")
                || normalized.contains("apikey") || normalized.contains("api_key");
    }

    private String value(Object value) {
        if (value == null || value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum<?> || value instanceof Temporal) {
            return String.valueOf(value);
        }
        if (value.getClass().isArray()) {
            return "[array length=" + Array.getLength(value) + ']';
        }
        if (value instanceof Iterable<?> iterable) {
            return "[collection size=" + (iterable instanceof java.util.Collection<?> collection
                    ? collection.size() : "unknown") + ']';
        }
        if (value.getClass().isAnnotationPresent(Entity.class)) {
            return value.getClass().getSimpleName() + "#" + entityId(value);
        }
        return value.getClass().getSimpleName();
    }

    private Object entityId(Object entity) {
        try {
            Method getId = entity.getClass().getMethod("getId");
            return getId.invoke(entity);
        } catch (ReflectiveOperationException ignored) {
            return "unknown";
        }
    }
}
