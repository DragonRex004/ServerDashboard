package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;

/**
 * PostgreSQL-Datenbankprozessor basierend auf der gemeinsamen SQL-Implementierung
 */
public class PostgresProcessor extends AbstractSQLProcessor {

    public PostgresProcessor(Database database) {
        super(database);
    }

    @Override
    protected String getDatabaseType() {
        return "PostgreSQL";
    }
}
