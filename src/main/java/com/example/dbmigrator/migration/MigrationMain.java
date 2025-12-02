package com.example.dbmigrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;

public class MigrationMain {

    private static final Logger log = LoggerFactory.getLogger(MigrationMain.class);

    public static void main(String[] args) {
        log.info("Starting DB migrations...");

        DataSource dataSource = DataSourceFactory.createDataSource();
        MigrationLoader loader = new MigrationLoader();
        var migrations = loader.loadMigrations();

        log.info("Found {} migrations", migrations.size());

        MigrationExecutor executor = new MigrationExecutor(dataSource);

        try {
            executor.migrate(migrations);
            log.info("Migrations finished successfully");

            try (var conn = dataSource.getConnection();
                 Statement st = conn.createStatement();
                 var rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
                    log.info("Users in DB: {}", rs.getInt(1));
                }
            } catch (Exception e) {
                log.warn("Check query failed: {}", e.getMessage());
            }

        } catch (SQLException e) {
            log.error("Migration error: {}", e.getMessage(), e);
        }
    }
}