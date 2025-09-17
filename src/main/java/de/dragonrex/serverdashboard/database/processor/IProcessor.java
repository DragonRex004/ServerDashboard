package de.dragonrex.serverdashboard.database.processor;

import de.dragonrex.serverdashboard.database.DatabaseResult;

import java.sql.Connection;

public interface IProcessor {

    void connect();
    void disconnect();
    DatabaseResult query(String query, Object... args);
    DatabaseResult update(String query, Object... args);
    Connection getConnection();

}
