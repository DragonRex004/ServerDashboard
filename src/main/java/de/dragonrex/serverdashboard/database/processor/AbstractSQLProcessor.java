package de.dragonrex.serverdashboard.database.processor;

import com.zaxxer.hikari.pool.HikariPool;
import de.dragonrex.serverdashboard.database.Database;
import de.dragonrex.serverdashboard.database.DatabaseResult;
import de.dragonrex.serverdashboard.database.SQLUtility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractSQLProcessor implements IProcessor {
    protected final Database database;
    protected HikariPool pool;

    public AbstractSQLProcessor(Database database) {
        this.database = database;
    }

    @Override
    public void connect() {
        try {
            this.pool = new HikariPool(this.database.getConfig());
            try (Connection connection = this.pool.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                statement.setQueryTimeout(15);
                statement.executeQuery();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish database connection for " + getDatabaseType(), e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (this.pool != null && !this.pool.getConnection().isClosed()) {
                try {
                    this.pool.shutdown();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to shutdown connection pool for " + getDatabaseType(), e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DatabaseResult query(String query, Object... args) {
        try (Connection connection = this.pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            return SQLUtility.getDatabaseResult(statement);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query on " + getDatabaseType() + ": " + query, e);
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

            return createUpdateResult(affectedRows);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update on " + getDatabaseType() + ": " + query, e);
        }
    }

    @Override
    public Connection getConnection() {
        try {
            return this.pool != null ? this.pool.getConnection() : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connection from pool for " + getDatabaseType(), e);
        }
    }

    private DatabaseResult createUpdateResult(int affectedRows) {
        DatabaseResult.Row row = new DatabaseResult.Row();
        row.put("affectedRows", affectedRows);
        return new DatabaseResult(java.util.List.of(row));
    }

    protected abstract String getDatabaseType();
}
