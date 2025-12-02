package com.example.dbmigrator.migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationLoaderTest {

    @Test
    void shouldLoadAndSortMigrationsByVersion() {
        MigrationLoader loader = new MigrationLoader();

        List<Migration> migrations = loader.loadMigrations();

        assertFalse(migrations.isEmpty(), "Migrations must not be empty");

        // Проверяем сортировку
        for (int i = 1; i < migrations.size(); i++) {
            assertTrue(
                    migrations.get(i - 1).version() < migrations.get(i).version(),
                    "Migrations must be sorted by version ascending"
            );
        }

        // Проверим, что имена и описания не пустые
        migrations.forEach(m -> {
            assertNotNull(m.scriptName());
            assertFalse(m.scriptName().isBlank());
            assertNotNull(m.description());
        });
    }
}