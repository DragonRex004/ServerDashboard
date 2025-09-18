package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.Database;

public class MariaProcessor extends AbstractSQLProcessor {

    public MariaProcessor(Database database) {
        super(database);
    }

    @Override
    protected String getDatabaseType() {
        return "MariaDB";
    }
}
