package de.dragonrex.serverdashboard;

import de.dragonrex.serverdashboard.config.AppConfig;
import de.dragonrex.serverdashboard.config.Config;
import de.dragonrex.serverdashboard.config.ConfigManager;
import de.dragonrex.serverdashboard.controller.ApiController;
import de.dragonrex.serverdashboard.controller.DashboardController;
import de.dragonrex.serverdashboard.controller.LoginController;
import de.dragonrex.serverdashboard.controller.WebSocketController;
import de.dragonrex.serverdashboard.database.Database;
import de.dragonrex.serverdashboard.database.DatabaseHandler;
import de.dragonrex.serverdashboard.database.processor.*;
import de.dragonrex.serverdashboard.user.UserManager;
import io.javalin.Javalin;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class ServerDashboard {
    private static final Logger LOGGER = Logger.getLogger(ServerDashboard.class.getName());

    private final AppConfig appConfig;
    private final ConfigManager configManager;
    private final Database database;
    private final DatabaseHandler databaseHandler;
    private final UserManager userManager;
    private final Javalin app;

    public ServerDashboard() {
        this.appConfig = new AppConfig("config/config.json");

        configureLogging();

        LOGGER.info("=== " + appConfig.getApplicationName() + " v" + appConfig.getApplicationVersion() + " wird gestartet ===");
        LOGGER.info("Umgebung: " + appConfig.getEnvironment());

        this.configManager = new ConfigManager();
        Path databaseConfigPath = Path.of("config/database.json");
        this.configManager.initializeConfig(databaseConfigPath);
        Config databaseConfig = this.configManager.loadConfig(databaseConfigPath);
        databaseConfig.addDefault("type", "SQLITE");
        databaseConfig.addDefault("database", "database.db");

        String jdbcUrl = databaseConfig.config().getString("jdbc");
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            ensureDatabaseDirectoryExists(jdbcUrl);
        }

        this.database = new Database(jdbcUrl);
        if (this.database.getNoSQL().isEmpty() || !this.database.getNoSQL().get()) {
            this.database.configure();
        }

        IProcessor processor = createDatabaseProcessor(databaseConfig);

        try {
            processor.connect();
            LOGGER.info("Datenbankverbindung hergestellt: " + databaseConfig.config().getString("type"));
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Verbinden zur Datenbank: " + e.getMessage());

            if ("MONGODB".equals(databaseConfig.config().getString("type"))) {
                LOGGER.info("Fallback zu SQLite-Datenbank...");
                this.database.configure();
                processor = new SQLiteProcessor(this.database);
                processor.connect();
                LOGGER.info("SQLite-Fallback-Verbindung hergestellt");
            } else {
                throw new RuntimeException("Datenbankverbindung fehlgeschlagen", e);
            }
        }

        this.databaseHandler = new DatabaseHandler(database, processor);
        this.userManager = new UserManager(databaseHandler, appConfig);
        this.userManager.loadUser();

        this.app = createJavalinApp();

        new LoginController(app, this.userManager, appConfig);
        new DashboardController(app, appConfig);
        new ApiController(app, appConfig);
        new WebSocketController(app);

        LOGGER.info("Server gestartet auf Port: " + appConfig.getApplicationPort());
        LOGGER.info("Dashboard verfügbar unter: http://localhost:" + appConfig.getApplicationPort());

        if (appConfig.isDevelopment()) {
            LOGGER.info("=== ENTWICKLUNGSMODUS AKTIV ===");
            LOGGER.info("Admin-Login: " + appConfig.getAdminConfig().username() + " / " + appConfig.getAdminConfig().password());
        }
    }

    private void configureLogging() {
        Logger rootLogger = Logger.getLogger("");

        Level logLevel = switch (appConfig.getLoggingLevel().toUpperCase()) {
            case "DEBUG" -> Level.FINE;
            case "INFO" -> Level.INFO;
            case "WARN" -> Level.WARNING;
            case "ERROR" -> Level.SEVERE;
            default -> Level.INFO;
        };

        rootLogger.setLevel(logLevel);

        if (!appConfig.isConsoleEnabled()) {
            rootLogger.getHandlers()[0].setLevel(Level.OFF);
        }

        LOGGER.info("Logging konfiguriert - Level: " + appConfig.getLoggingLevel() + 
                   ", Console: " + appConfig.isConsoleEnabled() + 
                   ", File: " + appConfig.isFileEnabled());
    }

    private IProcessor createDatabaseProcessor(Config databaseConfig) {
        return switch (databaseConfig.config().getString("type").toUpperCase()) {
            case "MYSQL" -> {
                LOGGER.info("MySQL-Datenbankprozessor wird verwendet");
                yield new MySQLProcessor(this.database);
            }
            case "POSTGRESQL" -> {
                LOGGER.info("PostgreSQL-Datenbankprozessor wird verwendet");
                yield new PostgresProcessor(this.database);
            }
            case "MARIADB" -> {
                LOGGER.info("MariaDB-Datenbankprozessor wird verwendet");
                yield new MariaProcessor(this.database);
            }
            case "MONGODB" -> {
                LOGGER.info("MongoDB-Datenbankprozessor wird verwendet");
                yield new MongoProcessor(this.database, databaseConfig.config().getString("database"));
            }
            default -> {
                LOGGER.info("SQLite-Datenbankprozessor wird verwendet (Standard)");
                yield new SQLiteProcessor(this.database);
            }
        };
    }

    private Javalin createJavalinApp() {
        return Javalin.create(config -> {
            config.staticFiles.add("/public");

            if (appConfig.isDevelopment()) {
                config.bundledPlugins.enableDevLogging();
                config.showJavalinBanner = true;
            } else {
                config.showJavalinBanner = false;
            }

        }).start(appConfig.getApplicationPort());
    }

    private void ensureDatabaseDirectoryExists(String jdbcUrl) {
        String filePath = jdbcUrl.substring("jdbc:sqlite:".length());
        Path databasePath = Paths.get(filePath);
        Path parentDir = databasePath.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
                LOGGER.info("Datenbankverzeichnis erstellt: " + parentDir.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.severe("Konnte Datenbankverzeichnis nicht erstellen: " + parentDir.toAbsolutePath());
                throw new RuntimeException("Konnte Datenbankverzeichnis nicht erstellen: " + parentDir.toAbsolutePath(), e);
            }
        }
    }

    public void shutdown() {
        LOGGER.info("Server wird heruntergefahren...");
        if (app != null) {
            app.stop();
        }
        if (databaseHandler != null && databaseHandler.processor() != null) {
            databaseHandler.processor().disconnect();
        }
        LOGGER.info("Server erfolgreich heruntergefahren");
    }

    public static void main(String[] args) {
        try {
            ServerDashboard dashboard = new ServerDashboard();

            Runtime.getRuntime().addShutdownHook(new Thread(dashboard::shutdown));

        } catch (Exception e) {
            LOGGER.severe("Fehler beim Starten der Anwendung: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
