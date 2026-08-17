package de.bohnottensen.financialaudit.infrastructure.config;

import de.bohnottensen.financialaudit.application.usecase.audit.AutomaticAuditEventListener;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AutomaticAuditHibernateConfiguration {
    @Bean
    HibernatePropertiesCustomizer automaticAuditCustomizer(AutomaticAuditEventListener listener) {
        return properties -> properties.put("hibernate.integrator_provider",
                (IntegratorProvider) () -> List.of(new Integrator() {
                    @Override
                    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
                                           SessionFactoryServiceRegistry serviceRegistry) {
                        EventListenerRegistry registry = serviceRegistry.getService(EventListenerRegistry.class);
                        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(listener);
                        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(listener);
                        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(listener);
                    }

                    @Override
                    public void disintegrate(SessionFactoryImplementor sessionFactory,
                                             SessionFactoryServiceRegistry serviceRegistry) {
                    }
                }));
    }
}
