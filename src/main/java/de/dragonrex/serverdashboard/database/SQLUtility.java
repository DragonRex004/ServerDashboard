package de.dragonrex.serverdashboard.database;

import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLUtility {

    @NotNull
    public static DatabaseResult getDatabaseResult(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        List<DatabaseResult.Row> rows = new ArrayList<>();
        try {
            while (resultSet.next()) {
                DatabaseResult.Row row = new DatabaseResult.Row();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    String columnName = resultSet.getMetaData().getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read database result", e);
        }
        return new DatabaseResult(rows);
    }
}
