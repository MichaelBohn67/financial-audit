package de.bohnottensen.financialaudit.infrastructure.config;

import de.bohnottensen.financialaudit.application.usecase.audit.AutomaticAuditEventListener;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AutomaticAuditHibernateConfigurationTest {

    @Test
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
        integrator.disintegrate(null, null);
    }
}
