package de.bohnottensen.financialaudit.infrastructure.config;

import de.bohnottensen.financialaudit.application.usecase.audit.AutomaticAuditEventListener;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AutomaticAuditHibernateConfigurationTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldRegisterAuditEventListenerWithHibernate() {
        AutomaticAuditEventListener listener = mock(AutomaticAuditEventListener.class);
        AutomaticAuditHibernateConfiguration config = new AutomaticAuditHibernateConfiguration();

        HibernatePropertiesCustomizer customizer = config.automaticAuditCustomizer(listener);
        Map<String, Object> properties = new HashMap<>();
        customizer.customize(properties);

        assertThat(properties).containsKey("hibernate.integrator_provider");
        IntegratorProvider provider = (IntegratorProvider) properties.get("hibernate.integrator_provider");
        List<Integrator> integrators = provider.getIntegrators();
        assertThat(integrators).hasSize(1);

        Integrator integrator = integrators.get(0);
        assertThat(integrator).isNotNull();

        EventListenerRegistry registry = mock(EventListenerRegistry.class);
        EventListenerGroup postInsertGroup = mock(EventListenerGroup.class);
        EventListenerGroup postUpdateGroup = mock(EventListenerGroup.class);
        EventListenerGroup postDeleteGroup = mock(EventListenerGroup.class);

        when(registry.getEventListenerGroup(EventType.POST_INSERT)).thenReturn(postInsertGroup);
        when(registry.getEventListenerGroup(EventType.POST_UPDATE)).thenReturn(postUpdateGroup);
        when(registry.getEventListenerGroup(EventType.POST_DELETE)).thenReturn(postDeleteGroup);

        SessionFactoryServiceRegistry serviceRegistry = (SessionFactoryServiceRegistry) Proxy.newProxyInstance(
                SessionFactoryServiceRegistry.class.getClassLoader(),
                new Class<?>[]{SessionFactoryServiceRegistry.class},
                (proxy, method, args) -> {
                    if ("getService".equals(method.getName()) && args != null && args.length == 1 && EventListenerRegistry.class.equals(args[0])) {
                        return registry;
                    }
                    return null;
                }
        );

        integrator.integrate(null, null, serviceRegistry);

        verify(postInsertGroup).appendListener(listener);
        verify(postUpdateGroup).appendListener(listener);
        verify(postDeleteGroup).appendListener(listener);

        integrator.disintegrate(null, null);
    }
}
