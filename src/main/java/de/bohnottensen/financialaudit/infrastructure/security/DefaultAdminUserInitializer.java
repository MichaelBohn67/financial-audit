package de.bohnottensen.financialaudit.infrastructure.security;

import de.bohnottensen.financialaudit.domain.model.Role;
import de.bohnottensen.financialaudit.domain.model.User;
import de.bohnottensen.financialaudit.domain.model.UserRole;
import de.bohnottensen.financialaudit.infrastructure.persistence.RoleRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.UserRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Component
@Order(10)
public class DefaultAdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminUserInitializer.class);
    private static final Set<String> REJECTED_DEFAULT_PASSWORDS = Set.of("admin", "password", "changeme", "changeit");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultAdminUsername;
    private final String defaultAdminPassword;

    public DefaultAdminUserInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${security.default-admin.username:admin}") String defaultAdminUsername,
            @Value("${security.default-admin.password:}") String defaultAdminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultAdminUsername = defaultAdminUsername;
        // Fail fast instead of silently provisioning a well-known administrative account.
        // The previous ":admin" fallback shipped a guessable ADMIN credential whenever the
        // deployment forgot to supply the secret.
        if (defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
            throw new IllegalStateException(
                    "security.default-admin.password must be set to a non-empty value before startup");
        }
        if (REJECTED_DEFAULT_PASSWORDS.contains(defaultAdminPassword.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "security.default-admin.password must not be a well-known default value");
        }
        this.defaultAdminPassword = defaultAdminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeAdminUser();
    }

    public void initializeAdminUser() {
        if (userRepository.findByUsername(defaultAdminUsername).isEmpty()) {
            log.info("Creating default admin user '{}' upon initial application startup", defaultAdminUsername);
            User admin = new User();
            admin.setUsername(defaultAdminUsername);
            admin.setPasswordHash(passwordEncoder.encode(defaultAdminPassword));
            admin.setEmail("admin@financial-audit.local");
            admin.setFirstName("Admin");
            admin.setLastName("Administrator");
            admin.setEnabled(true);
            User savedAdmin = userRepository.save(admin);

            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ADMIN");
                role.setDescription("Full administrative access");
                return roleRepository.save(role);
            });

            if (!userRoleRepository.existsByUserIdAndRoleId(savedAdmin.getId(), adminRole.getId())) {
                UserRole userRole = new UserRole();
                userRole.setUserId(savedAdmin.getId());
                userRole.setRoleId(adminRole.getId());
                userRoleRepository.save(userRole);
            }
        }
    }
}
