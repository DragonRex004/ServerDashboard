package de.dragonrex.serverdashboard.database;

import de.dragonrex.serverdashboard.database.processor.IProcessor;

public record DatabaseHandler(Database database, IProcessor processor) {
}
