package de.bohnottensen.financialaudit.infrastructure.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

/** Readiness signal for the two resources required for controlled operations. */
@Component("operationalReadiness")
public class OperationalReadinessHealthIndicator implements HealthIndicator {
    private final DataSource dataSource;
    private final Path archiveDirectory;

    public OperationalReadinessHealthIndicator(DataSource dataSource,
            @Value("${financial-audit.reporting.archive-directory:./var/report-archive}") String archiveDirectory) {
        this.dataSource = dataSource;
        this.archiveDirectory = Path.of(archiveDirectory).toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        try {
            try (var connection = dataSource.getConnection()) {
                if (!connection.isValid(2)) return Health.down().withDetail("database", "not-ready").build();
            }
            Path parent = archiveDirectory.getParent();
            boolean writable = Files.exists(archiveDirectory)
                    ? Files.isWritable(archiveDirectory)
                    : (parent != null && Files.isWritable(parent));
            if (!writable) return Health.down().withDetail("archiveStorage", "not-writable").build();
            return Health.up().withDetail("archiveStorage", "writable").build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
