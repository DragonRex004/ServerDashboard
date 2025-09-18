package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;

public class MySQLProcessor extends AbstractSQLProcessor {

    public MySQLProcessor(Database database) {
        super(database);
    }

    @Override
    protected String getDatabaseType() {
        return "MySQL";
    }
}
