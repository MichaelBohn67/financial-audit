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

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void shouldExposeSecurityFilterChain() {
        assertThat(securityFilterChain).isNotNull();
        assertThat(securityFilterChain.getFilters()).isNotEmpty();
    }

    @Test
    void shouldConfigureAuditorLeadAuditorAndAdminUsers() {
        assertUser("auditor", "ROLE_AUDITOR");
        assertUser("lead", "ROLE_LEAD_AUDITOR");
        assertUser("admin", "ROLE_ADMIN");
    }

    /**
     * The test fixture supplies its own hash via src/test/resources/application.properties.
     * Production no longer carries an inline default for this property, so a deployment can
     * never silently start on a published credential.
     */
    @Test
    void shouldAllowAdminLoginWithTestFixtureCredential() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");
        assertThat(passwordEncoder.matches("admin", admin.getPassword())).isTrue();
    }

    private void assertUser(String username, String authority) {
        UserDetails user = userDetailsService.loadUserByUsername(username);

        assertThat(user.getAuthorities())
                .extracting(Object::toString)
                .containsExactly(authority);
    }
}
