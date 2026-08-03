package de.bohnottensen.financialaudit.infrastructure.config;

import de.bohnottensen.financialaudit.domain.model.AmlEngine;
import de.bohnottensen.financialaudit.domain.model.AmlRule;
import de.bohnottensen.financialaudit.domain.model.HighAmountRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class AnalyticsConfig {

    @Bean
    public AmlEngine amlEngine() {
        List<AmlRule> rules = List.of(new HighAmountRule(new BigDecimal("10000")));
        return new AmlEngine(rules);
    }
}
