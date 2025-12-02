package com.example.dbmigrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MigrationLoader {

    private static final Logger log = LoggerFactory.getLogger(MigrationLoader.class);

    private static final Pattern FILENAME_PATTERN = Pattern.compile("V(\\d+)__([^\\.]+)\\.sql");

    private static final String MIGRATIONS_DIR = "db/migration";

    public List<Migration> loadMigrations() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL dirUrl = cl.getResource(MIGRATIONS_DIR);

        if (dirUrl == null) {
            log.warn("Migrations directory '{}' not found in classpath", MIGRATIONS_DIR);
            return List.of();
        }

        List<Migration> migrations = new ArrayList<>();

        try {
            Path dirPath = toPath(dirUrl);

            try (Stream<Path> paths = Files.list(dirPath)) {
                migrations = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".sql"))
                        .map(this::loadSingleMigration)
                        .filter(m -> m != null)
                        .sorted(Comparator.comparingInt(Migration::version))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to load migrations from '{}': {}", MIGRATIONS_DIR, e.getMessage(), e);
        }

        log.info("Loaded {} migration file(s)", migrations.size());
        return migrations;
    }

    private Path toPath(URL url) throws URISyntaxException, IOException {
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            return Paths.get(url.toURI());
        }

        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            FileSystem fs = FileSystems.newFileSystem(url.toURI(), java.util.Collections.emptyMap());
            return fs.getPath(MIGRATIONS_DIR);
        }

        throw new IllegalStateException("Unsupported URL protocol for migrations dir: " + url);
    }

    private Migration loadSingleMigration(Path path) {
        String fileName = path.getFileName().toString();
        Matcher matcher = FILENAME_PATTERN.matcher(fileName);

        if (!matcher.matches()) {
            log.warn("Skipping file with invalid migration name: {}", fileName);
            return null;
        }

        int version = Integer.parseInt(matcher.group(1));
        String description = matcher.group(2).replace('_', ' ');

        try {
            String sql = Files.readString(path, StandardCharsets.UTF_8);
            String checksum = calculateChecksum(sql);

            log.debug("Loaded migration V{} ({}), checksum={}", version, fileName, checksum);

            return new Migration(
                    version,
                    description,
                    fileName,
                    checksum,
                    sql
            );
        } catch (IOException e) {
            log.error("Failed to read migration file {}: {}", fileName, e.getMessage(), e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to calculate checksum for {}: {}", fileName, e.getMessage(), e);
            return null;
        }
    }

    private String calculateChecksum(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}