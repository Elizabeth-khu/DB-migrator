package com.example.dbmigrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class MigrationExecutor {

    private static final Logger log = LoggerFactory.getLogger(MigrationExecutor.class);

    private final DataSource dataSource;
    private final MigrationRepository repository;

    public MigrationExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
        this.repository = new MigrationRepository();
    }

    public void migrate(List<Migration> migrations) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);

                repository.ensureMigrationsTable(conn);

                repository.acquireMigrationLock(conn);

                Map<Integer, String> applied = repository.loadAppliedChecksums(conn);

                for (Migration migration : migrations) {
                    int version = migration.version();
                    String checksum = migration.checksum();
                    String scriptName = migration.scriptName();

                    if (applied.containsKey(version)) {
                        String existingChecksum = applied.get(version);
                        if (!existingChecksum.equals(checksum)) {
                            throw new IllegalStateException(
                                    "Checksum mismatch for version " + version +
                                            " script " + scriptName +
                                            ". Expected: " + existingChecksum +
                                            ", actual: " + checksum
                            );
                        }
                        log.info("Skipping already applied migration V{} ({})", version, scriptName);
                        continue;
                    }

                    log.info("Applying migration V{} ({}) ...", version, scriptName);
                    applySingleMigration(conn, migration);
                    applied.put(version, checksum);
                }

                conn.commit();
                log.info("All migrations committed successfully");
            } catch (Exception e) {
                log.error("Migration failed: {}", e.getMessage(), e);
                conn.rollback();
                throw new SQLException("Migration failed, rolled back", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void applySingleMigration(Connection conn, Migration migration) throws SQLException {
        int version = migration.version();
        try {
            try (Statement st = conn.createStatement()) {
                String[] statements = migration.sql().split(";(\\s*\\r?\\n)");
                for (String raw : statements) {
                    String sql = raw.trim();
                    if (sql.isEmpty()) continue;
                    st.execute(sql);
                }
            }

            repository.insertMigrationRecord(
                    conn,
                    migration.version(),
                    migration.description(),
                    migration.scriptName(),
                    migration.checksum(),
                    true
            );

            log.info("Migration V{} applied successfully", version);
        } catch (Exception e) {
            log.error("Migration V{} failed: {}", version, e.getMessage(), e);

            repository.insertMigrationRecord(
                    conn,
                    migration.version(),
                    migration.description(),
                    migration.scriptName(),
                    migration.checksum(),
                    false
            );

            throw new SQLException("Failed to apply migration V" + version, e);
        }
    }
}