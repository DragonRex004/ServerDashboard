package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;

/**
 * MySQL-Datenbankprozessor basierend auf der gemeinsamen SQL-Implementierung
 */
public class MySQLProcessor extends AbstractSQLProcessor {

    public MySQLProcessor(Database database) {
        super(database);
    }

    @Override
    protected String getDatabaseType() {
        return "MySQL";
    }
}
