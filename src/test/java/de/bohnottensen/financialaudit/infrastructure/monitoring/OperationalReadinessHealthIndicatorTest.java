package de.bohnottensen.financialaudit.infrastructure.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationalReadinessHealthIndicatorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnHealthUpWhenDatabaseIsValidAndArchiveDirectoryExistsAndIsWritable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        Path archiveDir = tempDir.resolve("archive");
        archiveDir.toFile().mkdirs();

        OperationalReadinessHealthIndicator indicator =
                new OperationalReadinessHealthIndicator(dataSource, archiveDir.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("archiveStorage", "writable");
        verify(connection).isValid(eq(2));
        verify(connection).close();
    }

    @Test
    void shouldReturnHealthUpWhenArchiveDirectoryDoesNotExistButParentIsWritable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        Path archiveDir = tempDir.resolve("non-existing-subdir");

        OperationalReadinessHealthIndicator indicator =
                new OperationalReadinessHealthIndicator(dataSource, archiveDir.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("archiveStorage", "writable");
    }

    @Test
    void shouldReturnHealthDownWhenDatabaseConnectionIsNotValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        OperationalReadinessHealthIndicator indicator =
                new OperationalReadinessHealthIndicator(dataSource, tempDir.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "not-ready");
        verify(connection).close();
    }

    @Test
    void shouldReturnHealthDownWhenGetConnectionThrowsSQLException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        SQLException sqlException = new SQLException("Connection failed");
        when(dataSource.getConnection()).thenThrow(sqlException);

        OperationalReadinessHealthIndicator indicator =
                new OperationalReadinessHealthIndicator(dataSource, tempDir.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void shouldReturnHealthDownWhenConnectionIsValidThrowsSQLException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenThrow(new SQLException("Validation error"));

        OperationalReadinessHealthIndicator indicator =
                new OperationalReadinessHealthIndicator(dataSource, tempDir.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void shouldReturnHealthDownWhenArchiveDirectoryExistsButIsNotWritable() throws SQLException, IOException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        Path nonWritableDir = Files.createDirectory(tempDir.resolve("readonly-dir"));
        File dirFile = nonWritableDir.toFile();
        boolean readOnlySet = dirFile.setReadOnly();

        try {
            if (readOnlySet && !Files.isWritable(nonWritableDir)) {
                OperationalReadinessHealthIndicator indicator =
                        new OperationalReadinessHealthIndicator(dataSource, nonWritableDir.toString());

                Health health = indicator.health();

                assertThat(health.getStatus()).isEqualTo(Status.DOWN);
                assertThat(health.getDetails()).containsEntry("archiveStorage", "not-writable");
            }
        } finally {
            dirFile.setWritable(true);
        }
    }

    @Test
    void shouldReturnHealthDownWhenArchiveDirectoryDoesNotExistAndParentIsNotWritable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        // A non-existing path under a non-writable root directory (like /proc or /root or /sys)
        Path nonWritableParentChild = Path.of("/proc/non-existent-sub/archive");

        OperationalReadinessHealthIndicator indicator =
                new OperationalReadinessHealthIndicator(dataSource, nonWritableParentChild.toString());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("archiveStorage", "not-writable");
    }
}
