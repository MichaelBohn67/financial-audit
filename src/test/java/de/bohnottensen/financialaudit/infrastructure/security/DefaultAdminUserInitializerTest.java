package de.bohnottensen.financialaudit.infrastructure.security;

import de.bohnottensen.financialaudit.domain.model.Role;
import de.bohnottensen.financialaudit.domain.model.User;
import de.bohnottensen.financialaudit.domain.model.UserRole;
import de.bohnottensen.financialaudit.infrastructure.persistence.RoleRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.UserRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DefaultAdminUserInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultAdminUserInitializer initializer;

    @Test
    void shouldCreateDefaultAdminUserOnStartup() {
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertThat(adminOpt).isPresent();

        User admin = adminOpt.get();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.isEnabled()).isTrue();
        // The bootstrap password comes from security.default-admin.password
        // (src/test/resources/application.properties), never a hardcoded default.
        assertThat(passwordEncoder.matches("test-only-admin-secret", admin.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("admin", admin.getPasswordHash())).isFalse();

        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        List<UserRole> userRoles = userRoleRepository.findByUserId(admin.getId());
        assertThat(userRoles).isNotEmpty();
        assertThat(userRoles).anyMatch(ur -> ur.getRoleId().equals(adminRole.getId()));
    }

    @Test
    void shouldBeIdempotentWhenInvokedMultipleTimes() {
        long initialUserCount = userRepository.count();
        long initialRoleCount = roleRepository.count();
        long initialUserRoleCount = userRoleRepository.count();

        initializer.run(new DefaultApplicationArguments());

        assertThat(userRepository.count()).isEqualTo(initialUserCount);
        assertThat(roleRepository.count()).isEqualTo(initialRoleCount);
        assertThat(userRoleRepository.count()).isEqualTo(initialUserRoleCount);
    }

    /**
     * The previous ":admin" default silently provisioned a guessable ADMIN account.
     * Startup must now refuse to continue instead.
     */
    @Test
    void shouldRejectWellKnownDefaultAdminPassword() {
        assertThatThrownBy(() -> new DefaultAdminUserInitializer(
                userRepository, roleRepository, userRoleRepository, passwordEncoder, "admin", "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be a well-known default value");
    }

    @Test
    void shouldRejectMissingAdminPassword() {
        assertThatThrownBy(() -> new DefaultAdminUserInitializer(
                userRepository, roleRepository, userRoleRepository, passwordEncoder, "admin", "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be set to a non-empty value");
    }
}
