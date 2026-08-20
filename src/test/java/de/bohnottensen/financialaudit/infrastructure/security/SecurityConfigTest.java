package de.bohnottensen.financialaudit.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private InMemoryUserDetailsManager userDetailsService;

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void shouldExposeSecurityFilterChain() {
        assertThat(securityFilterChain).isNotNull();
        assertThat(securityFilterChain.getFilters()).isNotEmpty();
    }

    @Test
    void shouldConfigureAuditorLeadAuditorAndAdminUsers() {
        assertThat(securityConfig.userDetailsService()).isNotNull();

        assertUser("auditor", "ROLE_AUDITOR");
        assertUser("lead", "ROLE_LEAD_AUDITOR");
        assertUser("admin", "ROLE_ADMIN");
    }

    private void assertUser(String username, String authority) {
        UserDetails user = userDetailsService.loadUserByUsername(username);

        assertThat(user.getAuthorities())
                .extracting(Object::toString)
                .containsExactly(authority);
    }
}
