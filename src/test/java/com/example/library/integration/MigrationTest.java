package com.example.library.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.util.UUID;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class MigrationTest {
    @Test
    void upgradePreservesReadersAndLoanRelations() throws Exception {
        String url = System.getenv("TEST_DB_URL");
        String user = System.getenv().getOrDefault("TEST_DB_USER", "library");
        String password = System.getenv().getOrDefault("TEST_DB_PASSWORD", "library");
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        UUID reader = UUID.randomUUID(), book = UUID.randomUUID(), loan = UUID.randomUUID();
        try {
            Flyway.configure()
                    .dataSource(url, user, password)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .target("1")
                    .load()
                    .migrate();
            try (var connection = DriverManager.getConnection(url, user, password)) {
                connection.setSchema(schema);
                try (var statement =
                        connection.prepareStatement(
                                "INSERT INTO readers(id,name) VALUES(?,'legacy')")) {
                    statement.setObject(1, reader);
                    statement.executeUpdate();
                }
                try (var statement =
                        connection.prepareStatement(
                                "INSERT INTO books(id,title,author,total_copies,available_copies)"
                                    + " VALUES(?,'book','author',1,0)")) {
                    statement.setObject(1, book);
                    statement.executeUpdate();
                }
                try (var statement =
                        connection.prepareStatement(
                                "INSERT INTO loans(id,reader_id,book_id) VALUES(?,?,?)")) {
                    statement.setObject(1, loan);
                    statement.setObject(2, reader);
                    statement.setObject(3, book);
                    statement.executeUpdate();
                }
            }
            Flyway.configure()
                    .dataSource(url, user, password)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .load()
                    .migrate();
            try (var connection = DriverManager.getConnection(url, user, password)) {
                connection.setSchema(schema);
                try (var statement =
                        connection.prepareStatement(
                                "SELECT r.student_number,l.book_id FROM readers r JOIN loans l ON"
                                    + " l.reader_id=r.id WHERE l.id=?")) {
                    statement.setObject(1, loan);
                    try (var result = statement.executeQuery()) {
                        assertTrue(result.next());
                        assertEquals("LEGACY-" + reader, result.getString(1));
                        assertEquals(book, result.getObject(2, UUID.class));
                    }
                }
            }
        } finally {
            try (var connection = DriverManager.getConnection(url, user, password);
                    var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
