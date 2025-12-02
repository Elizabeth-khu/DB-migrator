# DB Migrator

Simple Java database migration tool inspired by Flyway.

## Description
This project is a plain Java application that manages database migrations.
It reads SQL migration files from the classpath, validates checksums,
and applies migrations transactionally.

## Features
- Automatic loading of SQL migration files
- Versioned migrations (V1, V2, V3...)
- Checksum validation
- Transactional execution
- Idempotent behavior
- One-to-One, One-to-Many, Many-to-Many relations
- Integration tests

## Technologies
- Java 17
- JDBC
- H2 Database
- HikariCP
- Maven
- JUnit 5
- SLF4J + Logback

## How to run
Run migrations from IDE:
Run tests:
```bash
mvn test