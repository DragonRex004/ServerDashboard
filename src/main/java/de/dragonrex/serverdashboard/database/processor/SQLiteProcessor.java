package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;
import de.dragonrex.serverdashboard.database.DatabaseResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteProcessor extends AbstractSQLProcessor {

    public SQLiteProcessor(Database database) {
        super(database);
    }

    @Override
    public void connect() {
        super.connect();

        try (Connection connection = this.pool.getConnection()) {
            configureSQLiteSettings(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to configure SQLite-specific settings", e);
        }
    }

    private void configureSQLiteSettings(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");

            statement.execute("PRAGMA synchronous = NORMAL");

            statement.execute("PRAGMA cache_size = -2000");

            statement.execute("PRAGMA temp_store = MEMORY");

            statement.execute("PRAGMA mmap_size = 134217728");

            statement.execute("PRAGMA optimize");

            statement.execute("PRAGMA foreign_keys = ON");

            statement.execute("PRAGMA auto_vacuum = INCREMENTAL");
        }
    }

    @Override
    public DatabaseResult update(String query, Object... args) {
        try (Connection connection = this.pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            int affectedRows = statement.executeUpdate();

            DatabaseResult.Row row = new DatabaseResult.Row();
            row.put("affectedRows", affectedRows);

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

    public void optimizeDatabase() {
        try (Connection connection = this.pool.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA optimize");
            statement.execute("PRAGMA incremental_vacuum");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to optimize SQLite database", e);
        }
    }

    public DatabaseResult getDatabaseStats() {
        try (Connection connection = this.pool.getConnection()) {
            DatabaseResult.Row stats = new DatabaseResult.Row();

            try (PreparedStatement statement = connection.prepareStatement("SELECT page_count * page_size as size FROM pragma_page_count(), pragma_page_size()")) {
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    stats.put("databaseSize", resultSet.getLong("size"));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("PRAGMA journal_mode")) {
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    stats.put("journalMode", resultSet.getString(1));
                }
            }

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
