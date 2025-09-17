package de.dragonrex.serverdashboard.user;

import de.dragonrex.serverdashboard.config.AppConfig;
import de.dragonrex.serverdashboard.database.DatabaseHandler;
import de.dragonrex.serverdashboard.database.DatabaseResult;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class UserManager {
    private final DatabaseHandler databaseHandler;
    private final List<User> userList;

    private final AppConfig appConfig;

    public UserManager(DatabaseHandler databaseHandler, AppConfig appConfig) {
        this.databaseHandler = databaseHandler;
        this.appConfig = appConfig;
        this.userList = new ArrayList<>();
        initializeUserTable();
    }

    public void loadUser() {
        if (isMongoDatabase()) {
            loadUsersFromMongo();
        } else {
            loadUsersFromSQL();
        }
    }

    private void loadUsersFromSQL() {
        this.userList.clear();
        DatabaseResult result = null;
        try {
            result = databaseHandler.processor().query("SELECT username, password FROM users");

            while (result.next()) {
                String username = result.getString("username");
                String password = result.getString("password");
                this.userList.add(new User(username, password));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Laden der Benutzer aus der SQL-Datenbank: " + e.getMessage());
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public boolean authenticateUser(String username, String password) {
        if (isMongoDatabase()) {
            return authenticateUserMongo(username, password);
        } else {
            return authenticateUserSQL(username, password);
        }
    }

    private boolean authenticateUserSQL(String username, String password) {
        DatabaseResult result = null;
        try {
            result = databaseHandler.processor().query(
                    "SELECT COUNT(*) as count FROM users WHERE username = ? AND password = ?",
                    username, password
            );

            if (result.next()) {
                int count = result.getInt("count");
                return count > 0;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler bei der SQL-Benutzerauthentifizierung: " + e.getMessage());
            return false;
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public boolean addUser(String username, String password) {
        try {
            DatabaseResult checkResult = databaseHandler.processor().query(
                    "SELECT COUNT(*) as count FROM users WHERE username = ?", username
            );

            if (checkResult.next() && checkResult.getInt("count") > 0) {
                checkResult.close();
                return false;
            }
            checkResult.close();

            DatabaseResult result = databaseHandler.processor().update(
                    "INSERT INTO users (username, password) VALUES (?, ?)",
                    username, password
            );
            result.close();

            loadUser();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Hinzufügen des Benutzers: " + e.getMessage());
            return false;
        }
    }

    private boolean addUserToMongo(String username, String password, String email, String role, List<String> permissions) {
        DatabaseResult result = null;

        try {
            result = databaseHandler.processor().query("users", "username:" + username);

            boolean userExists = false;
            if (result.next()) {
                userExists = true;
            }
            result.close();

            if (userExists) {
                System.out.println("MongoDB-Benutzer existiert bereits: " + username);
                return false;
            }

            String userDocument = String.format("""
                            {
                                "username": "%s",
                                "password": "%s",
                                "email": "%s",
                                "role": "%s",
                                "permissions": %s,
                                "created_at": "%s"
                            }
                            """,
                    username,
                    password,
                    email,
                    role,
                    formatPermissionsForMongo(permissions),
                    java.time.Instant.now().toString()
            );

            result = databaseHandler.processor().update("users", "INSERT", userDocument);
            result.close();

            System.out.println("MongoDB-Benutzer erfolgreich hinzugefügt: " + username + " (Rolle: " + role + ")");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Hinzufügen des MongoDB-Benutzers: " + e.getMessage());
            return false;
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    private String formatPermissionsForMongo(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < permissions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(permissions.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    public void loadUsersFromMongo() {
        this.userList.clear();
        DatabaseResult result = null;

        try {
            result = databaseHandler.processor().query("users");

            while (result.next()) {
                String username = result.getString("username");
                String password = result.getString("password");
                this.userList.add(new User(username, password));
            }

            System.out.println("MongoDB-Benutzer geladen: " + userList.size() + " Benutzer gefunden");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Laden der Benutzer aus MongoDB: " + e.getMessage());
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public boolean authenticateUserMongo(String username, String password) {
        DatabaseResult result = null;

        try {
            result = databaseHandler.processor().query("users", "username:" + username, "password:" + password);

            boolean authenticated = result.next();
            result.close();

            return authenticated;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler bei der MongoDB-Benutzerauthentifizierung: " + e.getMessage());
            return false;
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public boolean removeUser(String username) {
        try {
            DatabaseResult result = databaseHandler.processor().update(
                    "DELETE FROM users WHERE username = ?", username
            );
            result.close();

            loadUser();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Löschen des Benutzers: " + e.getMessage());
            return false;
        }
    }

    private void initializeUserTable() {
        if (isMongoDatabase()) {
            initializeMongoUsersCollection();
        } else {
            initializeSQLUsersTable();
        }
    }

    private boolean isMongoDatabase() {
        return databaseHandler.processor() instanceof de.dragonrex.serverdashboard.database.processor.MongoProcessor;
    }

    private void initializeMongoUsersCollection() {
        DatabaseResult countResult = null;

        try {
            countResult = databaseHandler.processor().query("users");

            int userCount = 0;
            while (countResult.next()) {
                userCount++;
            }

            if (userCount == 0) {
                loadUsersFromConfigForMongo();
                System.out.println("Standard-Benutzer aus config.json wurden zur MongoDB users collection hinzugefügt.");
            } else {
                System.out.println("MongoDB users collection enthält bereits " + userCount + " Benutzer.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Initialisieren der MongoDB users collection: " + e.getMessage());
            try {
                loadUsersFromConfigForMongo();
            } catch (Exception fallbackError) {
                System.err.println("Fallback fehlgeschlagen: " + fallbackError.getMessage());
            }
        } finally {
            if (countResult != null) {
                countResult.close();
            }
        }
    }

    private void initializeSQLUsersTable() {
        DatabaseResult createResult = null;
        DatabaseResult countResult = null;

        try {
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username VARCHAR(50) UNIQUE NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        email VARCHAR(100),
                        role VARCHAR(20) DEFAULT 'user',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            createResult = databaseHandler.processor().update(createTableSQL);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Erstellen der Benutzer-Tabelle: " + e.getMessage());
        } finally {
            if (createResult != null) {
                createResult.close();
            }
        }

        try {
            countResult = databaseHandler.processor().query("SELECT COUNT(*) as count FROM users");
            if (countResult.next() && countResult.getInt("count") == 0) {
                loadUsersFromConfig();

                System.out.println("Standard-Benutzer aus config.json wurden zur Datenbank hinzugefügt.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Prüfen/Hinzufügen der Standard-Benutzer: " + e.getMessage());
        } finally {
            if (countResult != null) {
                countResult.close();
            }
        }
    }

    private void loadUsersFromConfig() {
        try {
            AppConfig.AdminConfig admin = appConfig.getAdminConfig();
            addUserWithDetails(admin.username(), admin.password(), admin.email(), admin.role());
            System.out.println("Admin-Benutzer aus Konfiguration hinzugefügt: " + admin.username());

            for (AppConfig.UserConfig user : appConfig.getDefaultUsers()) {
                addUserWithDetails(user.username(), user.password(), user.email(), user.role());
                System.out.println("Standard-Benutzer aus Konfiguration hinzugefügt: " + user.username());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Laden der Benutzer aus AppConfig: " + e.getMessage());
            addUser("admin", "admin");
            addUser("user", "user");
        }
    }

    private void loadUsersFromConfigForMongo() {
        try {
            AppConfig.AdminConfig admin = appConfig.getAdminConfig();
            addUserToMongo(admin.username(), admin.password(), admin.email(), admin.role(), admin.permissions());
            System.out.println("Admin-Benutzer zu MongoDB hinzugefügt: " + admin.username());

            for (AppConfig.UserConfig user : appConfig.getDefaultUsers()) {
                addUserToMongo(user.username(), user.password(), user.email(), user.role(), user.permissions());
                System.out.println("Standard-Benutzer zu MongoDB hinzugefügt: " + user.username());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Laden der Benutzer in MongoDB: " + e.getMessage());
        }
    }

    public boolean addUserWithDetails(String username, String password, String email, String role) {
        if (password.length() < appConfig.getPasswordMinLength()) {
            System.err.println("Passwort zu kurz. Mindestlänge: " + appConfig.getPasswordMinLength());
            return false;
        }

        DatabaseResult checkResult = null;
        DatabaseResult insertResult = null;

        try {
            checkResult = databaseHandler.processor().query(
                    "SELECT COUNT(*) as count FROM users WHERE username = ?", username
            );

            if (checkResult.next() && checkResult.getInt("count") > 0) {
                System.out.println("Benutzer existiert bereits: " + username);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (checkResult != null) {
                checkResult.close();
            }
        }

        try {
            insertResult = databaseHandler.processor().update(
                    "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)",
                    username, password, email, role
            );

            loadUser();
            System.out.println("Benutzer erfolgreich hinzugefügt: " + username + " (Rolle: " + role + ")");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Hinzufügen des Benutzers mit Details: " + e.getMessage());
            return false;
        } finally {
            if (insertResult != null) {
                insertResult.close();
            }
        }
    }
}
