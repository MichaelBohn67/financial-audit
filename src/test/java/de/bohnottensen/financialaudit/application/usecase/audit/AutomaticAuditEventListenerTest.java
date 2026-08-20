package de.bohnottensen.financialaudit.application.usecase.audit;

import de.bohnottensen.financialaudit.domain.model.AuditEvent;
import de.bohnottensen.financialaudit.domain.model.Booking;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutomaticAuditEventListenerTest {

    private AutomaticAuditEventListener listener;
    private EventSource session;
    private List<Object> persistedEntities;
    private String[] propertyNames;
    private EntityPersister persister;

    @BeforeEach
    void setUp() {
        listener = new AutomaticAuditEventListener();
        persistedEntities = new ArrayList<>();
        propertyNames = new String[0];
        session = (EventSource) Proxy.newProxyInstance(
                EventSource.class.getClassLoader(),
                new Class<?>[]{EventSource.class},
                (proxy, method, args) -> {
                    if ("persist".equals(method.getName())) {
                        persistedEntities.add(args[0]);
                    }
                    return null;
                }
        );
        persister = (EntityPersister) Proxy.newProxyInstance(
                EntityPersister.class.getClassLoader(),
                new Class<?>[]{EntityPersister.class},
                (proxy, method, args) -> {
                    if ("getPropertyNames".equals(method.getName())) {
                        return propertyNames;
                    }
                    return null;
                }
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldHandlePostInsertForAuditedEntityWithAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor_bob", "pass", List.of(new SimpleGrantedAuthority("ROLE_AUDITOR")))
        );

        Booking booking = new Booking();
        booking.setId(99L);

        propertyNames = new String[]{"description", "amount", "secretToken"};
        Object[] state = new Object[]{"Invoice 123", new BigDecimal("500.00"), "my-secret-token"};

        PostInsertEvent event = new PostInsertEvent(booking, 99L, state, persister, session);

        listener.onPostInsert(event);

        assertThat(persistedEntities).hasSize(1);
        AuditEvent recorded = (AuditEvent) persistedEntities.get(0);

        assertThat(recorded.getEntityType()).isEqualTo("Booking");
        assertThat(recorded.getEntityId()).isEqualTo(99L);
        assertThat(recorded.getEventType()).isEqualTo("PERSISTENCE_CREATE");
        assertThat(recorded.getActor()).isEqualTo("auditor_bob");
        assertThat(recorded.getSummary()).isEqualTo("Automatic persistence audit event");
        assertThat(recorded.getPreviousValue()).isNull();
        assertThat(recorded.getCurrentValue()).isEqualTo("description=Invoice 123;amount=500.00;secretToken=[REDACTED];");
    }

    @Test
    void shouldHandlePostUpdateWithAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        );

        Booking booking = new Booking();
        booking.setId(99L);

        propertyNames = new String[]{"amount"};
        Object[] oldState = new Object[]{new BigDecimal("100.00")};
        Object[] newState = new Object[]{new BigDecimal("200.00")};

        PostUpdateEvent event = new PostUpdateEvent(booking, 99L, newState, oldState, null, persister, session);

        listener.onPostUpdate(event);

        assertThat(persistedEntities).hasSize(1);
        AuditEvent recorded = (AuditEvent) persistedEntities.get(0);

        assertThat(recorded.getEntityType()).isEqualTo("Booking");
        assertThat(recorded.getEntityId()).isEqualTo(99L);
        assertThat(recorded.getEventType()).isEqualTo("PERSISTENCE_UPDATE");
        assertThat(recorded.getActor()).isEqualTo("SYSTEM");
        assertThat(recorded.getPreviousValue()).isEqualTo("amount=100.00;");
        assertThat(recorded.getCurrentValue()).isEqualTo("amount=200.00;");
    }

    @Test
    void shouldHandlePostDeleteWithNullStateAndNullSecurityContext() {
        SecurityContextHolder.clearContext();

        Booking booking = new Booking();
        booking.setId(99L);

        propertyNames = new String[]{"apiKey", "items", "tags", "timestamp", "active", "refBooking"};
        Booking ref = new Booking();
        ref.setId(1234L);
        Object[] deletedState = new Object[]{
                "secret-api-key",
                new String[]{"a", "b"},
                List.of("tag1", "tag2", "tag3"),
                LocalDateTime.of(2026, 8, 1, 12, 0),
                true,
                ref
        };

        PostDeleteEvent event = new PostDeleteEvent(booking, 99L, deletedState, persister, session);

        listener.onPostDelete(event);

        assertThat(persistedEntities).hasSize(1);
        AuditEvent recorded = (AuditEvent) persistedEntities.get(0);

        assertThat(recorded.getEntityType()).isEqualTo("Booking");
        assertThat(recorded.getEntityId()).isEqualTo(99L);
        assertThat(recorded.getEventType()).isEqualTo("PERSISTENCE_DELETE");
        assertThat(recorded.getActor()).isEqualTo("SYSTEM");
        assertThat(recorded.getCurrentValue()).isNull();
        assertThat(recorded.getPreviousValue())
                .contains("apiKey=[REDACTED];")
                .contains("items=[array length=2];")
                .contains("tags=[collection size=3];")
                .contains("timestamp=2026-08-01T12:00;")
                .contains("active=true;")
                .contains("refBooking=Booking#1234;");
    }

    @Test
    void shouldIgnoreNonAuditedEntitiesAndNullIdAndAuditEvents() {
        Object nonAudited = new Object();
        PostInsertEvent event1 = new PostInsertEvent(nonAudited, 1L, new Object[]{}, persister, session);
        listener.onPostInsert(event1);

        AuditEvent auditEvent = new AuditEvent();
        PostInsertEvent event2 = new PostInsertEvent(auditEvent, 1L, new Object[]{}, persister, session);
        listener.onPostInsert(event2);

        Booking bookingNullId = new Booking();
        PostInsertEvent event3 = new PostInsertEvent(bookingNullId, null, new Object[]{}, persister, session);
        listener.onPostInsert(event3);

        assertThat(persistedEntities).isEmpty();
        assertThat(listener.requiresPostCommitHandling(persister)).isFalse();
    }
}
