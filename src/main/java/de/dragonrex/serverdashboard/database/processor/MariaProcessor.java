package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;

/**
 * MariaDB-Datenbankprozessor basierend auf der gemeinsamen SQL-Implementierung
 */
public class MariaProcessor extends AbstractSQLProcessor {

    public MariaProcessor(Database database) {
        super(database);
    }

    @Override
    protected String getDatabaseType() {
        return "MariaDB";
    }
}
