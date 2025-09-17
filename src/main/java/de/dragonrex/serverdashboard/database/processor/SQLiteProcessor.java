package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;
import de.dragonrex.serverdashboard.database.DatabaseResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite-Datenbankprozessor mit SQLite-spezifischen Optimierungen
 * Konfiguriert SQLite für beste Performance und Zuverlässigkeit
 */
public class SQLiteProcessor extends AbstractSQLProcessor {

    public SQLiteProcessor(Database database) {
        super(database);
    }

    @Override
    public void connect() {
        // Basis-Verbindung herstellen
        super.connect();

        // SQLite-spezifische Optimierungen anwenden
        try (Connection connection = this.pool.getConnection()) {
            configureSQLiteSettings(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to configure SQLite-specific settings", e);
        }
    }

    /**
     * Konfiguriert SQLite-spezifische Einstellungen für optimale Performance und Zuverlässigkeit
     */
    private void configureSQLiteSettings(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // WAL-Modus für bessere Concurrency und Performance
            statement.execute("PRAGMA journal_mode = WAL");

            // Synchronous-Modus für Balance zwischen Performance und Sicherheit
            statement.execute("PRAGMA synchronous = NORMAL");

            // Cache-Größe erhöhen für bessere Performance (2MB)
            statement.execute("PRAGMA cache_size = -2000");

            // Temp-Dateien im Speicher halten
            statement.execute("PRAGMA temp_store = MEMORY");

            // Memory-mapped I/O für große Datenbanken (128MB)
            statement.execute("PRAGMA mmap_size = 134217728");

            // Optimize-Pragma für bessere Query-Performance
            statement.execute("PRAGMA optimize");

            // Foreign Key Constraints aktivieren
            statement.execute("PRAGMA foreign_keys = ON");

            // Auto-Vacuum für automatische Datenbankbereinigung
            statement.execute("PRAGMA auto_vacuum = INCREMENTAL");
        }
    }

    @Override
    public DatabaseResult update(String query, Object... args) {
        try (Connection connection = this.pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Setze Parameter
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            // Führe Update aus
            int affectedRows = statement.executeUpdate();

            // SQLite-spezifische Informationen hinzufügen
            DatabaseResult.Row row = new DatabaseResult.Row();
            row.put("affectedRows", affectedRows);

            // Last Insert Row ID für INSERT-Operationen
            if (query.trim().toUpperCase().startsWith("INSERT")) {
                try (Statement lastIdStatement = connection.createStatement()) {
                    var resultSet = lastIdStatement.executeQuery("SELECT last_insert_rowid()");
                    if (resultSet.next()) {
                        row.put("lastInsertRowId", resultSet.getLong(1));
                    }
                }
            }

            return new DatabaseResult(java.util.List.of(row));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQLite update: " + query, e);
        }
    }

    /**
     * Führt eine Datenbank-Optimierung durch (sollte regelmäßig aufgerufen werden)
     */
    public void optimizeDatabase() {
        try (Connection connection = this.pool.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA optimize");
            statement.execute("PRAGMA incremental_vacuum");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to optimize SQLite database", e);
        }
    }

    /**
     * Gibt SQLite-spezifische Datenbankstatistiken zurück
     */
    public DatabaseResult getDatabaseStats() {
        try (Connection connection = this.pool.getConnection()) {
            DatabaseResult.Row stats = new DatabaseResult.Row();

            // Datenbankgröße
            try (PreparedStatement statement = connection.prepareStatement("SELECT page_count * page_size as size FROM pragma_page_count(), pragma_page_size()")) {
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    stats.put("databaseSize", resultSet.getLong("size"));
                }
            }

            // Journal-Modus
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA journal_mode")) {
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    stats.put("journalMode", resultSet.getString(1));
                }
            }

            // Cache-Größe
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA cache_size")) {
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    stats.put("cacheSize", resultSet.getInt(1));
                }
            }

            return new DatabaseResult(java.util.List.of(stats));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get SQLite database statistics", e);
        }
    }

    @Override
    protected String getDatabaseType() {
        return "SQLite";
    }
}
