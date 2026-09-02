package de.bohnottensen.financialaudit.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/bookings/**").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/sampling").authenticated()
                        .requestMatchers("/api/**", "/open-banking/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> {})
                .formLogin(formLogin -> {});
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            @Value("${security.users.auditor.password-hash}") String auditorPasswordHash,
            @Value("${security.users.lead-auditor.password-hash}") String leadAuditorPasswordHash,
            @Value("${security.users.admin.password-hash}") String adminPasswordHash) {
        UserDetails auditor = User.withUsername("auditor")
                .password(auditorPasswordHash)
                .roles("AUDITOR")
                .build();
        UserDetails leadAuditor = User.withUsername("lead")
                .password(leadAuditorPasswordHash)
                .roles("LEAD_AUDITOR")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password(adminPasswordHash)
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(auditor, leadAuditor, admin);
    }
}
