package de.dragonrex.serverdashboard.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseResult {
    private final List<Row> rows;
    private int currentRow = -1;

    public DatabaseResult(List<Row> rows) {
        this.rows = rows;
    }

    public String getString(String column) {
        Object value = getCurrentRow().get(column);
        return value != null ? value.toString() : null;
    }

    public int getInt(String column) {
        Object value = getCurrentRow().get(column);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public boolean next() {
        currentRow++;
        return currentRow < rows.size();
    }

    public void close() {
        try {
            if (rows != null && !rows.isEmpty()) {
                rows.clear();
            }
        } catch (UnsupportedOperationException e) {
            System.out.println("DatabaseResult: Liste ist unveränderlich, close() übersprungen");
        }
    }

    private Row getCurrentRow() {
        if (currentRow >= 0 && currentRow < rows.size()) {
            return rows.get(currentRow);
        }
        return new Row();
    }

    public static class Row {
        private final Map<String, Object> data = new HashMap<>();

        public void put(String column, Object value) {
            data.put(column, value);
        }

        public Object get(String column) {
            return data.get(column);
        }
    }
}