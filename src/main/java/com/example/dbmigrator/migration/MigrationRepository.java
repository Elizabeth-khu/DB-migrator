package com.example.dbmigrator.migration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MigrationRepository {

    public void ensureMigrationsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS schema_migration (
                version INT PRIMARY KEY,
                description VARCHAR(255),
                script VARCHAR(255),
                checksum VARCHAR(64),
                installed_on TIMESTAMP,
                success BOOLEAN
            );
            """;

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public Map<Integer, String> loadAppliedChecksums(Connection conn) throws SQLException {
        Map<Integer, String> result = new HashMap<>();
        String sql = "SELECT version, checksum FROM schema_migration WHERE success = TRUE";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getInt("version"), rs.getString("checksum"));
            }
        }
        return result;
    }

    public void insertMigrationRecord(
            Connection conn,
            int version,
            String description,
            String script,
            String checksum,
            boolean success
    ) throws SQLException {
        String sql = """
            INSERT INTO schema_migration (version, description, script, checksum, installed_on, success)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, version);
            ps.setString(2, description);
            ps.setString(3, script);
            ps.setString(4, checksum);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBoolean(6, success);
            ps.executeUpdate();
        }
    }

    public void acquireMigrationLock(Connection conn) throws SQLException {
        String createLockTable = """
            CREATE TABLE IF NOT EXISTS migration_lock (
                id INT PRIMARY KEY
            );
            """;

        try (Statement st = conn.createStatement()) {
            st.execute(createLockTable);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "MERGE INTO migration_lock (id) KEY(id) VALUES (1)"
        )) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM migration_lock WHERE id = 1 FOR UPDATE"
        )) {
            ps.executeQuery();
        }
    }
}