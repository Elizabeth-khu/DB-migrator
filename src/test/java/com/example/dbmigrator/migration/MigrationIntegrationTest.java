package com.example.dbmigrator.migration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationIntegrationTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        // отдельная in-memory БД для тестов
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(3);

        dataSource = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void migrationsShouldApplySuccessfullyAndBeIdempotent() throws Exception {
        // given
        MigrationLoader loader = new MigrationLoader();
        List<Migration> migrations = loader.loadMigrations();
        assertFalse(migrations.isEmpty(), "Migrations list must not be empty");

        MigrationExecutor executor = new MigrationExecutor(dataSource);

        // when: первый запуск миграций
        executor.migrate(migrations);

        // then: проверяем, что таблицы созданы и данные есть
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {

            // проверяем, что таблица schema_migration содержит все версии
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM schema_migration WHERE success = TRUE")) {
                assertTrue(rs.next());
                int appliedCount = rs.getInt(1);
                assertEquals(migrations.size(), appliedCount, "All migrations must be applied");
            }

            // проверяем, что пользователи созданы (из V2)
            int userCount;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                assertTrue(rs.next());
                userCount = rs.getInt(1);
                assertTrue(userCount >= 50, "There should be at least 50 users");
            }

            int postCount;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM posts")) {
                assertTrue(rs.next());
                postCount = rs.getInt(1);
                assertTrue(postCount > 0, "There should be at least 1 post");
            }

            int totalCount;
            try (ResultSet rs = st.executeQuery("""
                    SELECT
                        (SELECT COUNT(*) FROM users) +
                        (SELECT COUNT(*) FROM posts) +
                        (SELECT COUNT(*) FROM roles) +
                        (SELECT COUNT(*) FROM user_roles)
                        """)) {
                assertTrue(rs.next());
                totalCount = rs.getInt(1);
                assertTrue(totalCount>=100,"Total number of rows in DB should be at least 100");
            }
        }

        // when: второй запуск мигратора (идемпотентность)
        executor.migrate(migrations);

        // then: количество строк в schema_migration не изменилось
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM schema_migration WHERE success = TRUE")) {

            assertTrue(rs.next());
            int appliedCountAfter = rs.getInt(1);
            assertEquals(migrations.size(), appliedCountAfter,
                    "Number of applied migrations should remain the same after rerun");
        }
    }

    @Test
    void shouldFailWhenChecksumDoesNotMatch() throws Exception {
        MigrationLoader loader = new MigrationLoader();
        List<Migration> migrations = loader.loadMigrations();
        MigrationExecutor executor = new MigrationExecutor(dataSource);

        // 1. первый запуск — всё применится
        executor.migrate(migrations);

        // 2. ломаем checksum для первой миграции
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {

            int updated = st.executeUpdate("""
            UPDATE schema_migration
            SET checksum = 'deadbeef'
            WHERE version = 1
            """);
            assertEquals(1, updated, "One row in schema_migration should be updated");
        }

        // 3. второй запуск — ожидаем ошибку checksum
        Exception ex = assertThrows(Exception.class, () -> executor.migrate(migrations));

        // можно чуть точнее проверить сообщение
        String msg = ex.getMessage();
        assertTrue(msg.contains("Checksum mismatch") || (ex.getCause() != null && ex.getCause().getMessage().contains("Checksum mismatch")),
                "Exception should indicate checksum mismatch");
    }
}