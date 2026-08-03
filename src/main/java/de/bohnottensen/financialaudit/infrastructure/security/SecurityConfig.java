package de.bohnottensen.financialaudit.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/bookings/**").authenticated()
                        .requestMatchers("/api/**", "/open-banking/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> {})
                .formLogin(formLogin -> {});
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails assistant = User.withUsername("assistant")
                .password("{noop}assistant")
                .roles("ASSISTANT")
                .build();
        UserDetails seniorAuditor = User.withUsername("senior")
                .password("{noop}senior")
                .roles("SENIOR_AUDITOR")
                .build();
        UserDetails wirtschaftspruefer = User.withUsername("wirtschaftspruefer")
                .password("{noop}wirtschaftspruefer")
                .roles("WIRTSCHAFTSPRUEFER")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password("{noop}admin")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(assistant, seniorAuditor, wirtschaftspruefer, admin);
    }
}
