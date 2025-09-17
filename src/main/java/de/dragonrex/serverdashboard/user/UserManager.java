package de.dragonrex.serverdashboard.user;

import de.dragonrex.serverdashboard.config.AppConfig;
import de.dragonrex.serverdashboard.database.DatabaseHandler;
import de.dragonrex.serverdashboard.database.DatabaseResult;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            // Prüfen ob Benutzer bereits existiert
            DatabaseResult checkResult = databaseHandler.processor().query(
                "SELECT COUNT(*) as count FROM users WHERE username = ?", username
            );

            if (checkResult.next() && checkResult.getInt("count") > 0) {
                checkResult.close();
                return false; // Benutzer existiert bereits
            }
            checkResult.close();

            // Neuen Benutzer hinzufügen
            DatabaseResult result = databaseHandler.processor().update(
                "INSERT INTO users (username, password) VALUES (?, ?)", 
                username, password
            );
            result.close();

            // Benutzerliste neu laden
            loadUser();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Hinzufügen des Benutzers: " + e.getMessage());
            return false;
        }
    }

        /**
         * Fügt einen Benutzer zur MongoDB users collection hinzu
         */
        private boolean addUserToMongo(String username, String password, String email, String role, List<String> permissions) {
            DatabaseResult result = null;

            try {
                // Prüfe ob Benutzer bereits existiert
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

                // Erstelle Benutzer-Dokument
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

                // Benutzer in MongoDB einfügen
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

        /**
         * Formatiert Berechtigungen für MongoDB-JSON
         */
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

        /**
         * Lädt Benutzer aus MongoDB users collection
         */
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

        /**
         * Authentifiziert Benutzer gegen MongoDB users collection
         */
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

            // Benutzerliste neu laden
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
            // Prüfe ob users collection bereits Dokumente enthält
            countResult = databaseHandler.processor().query("users");

            // Zähle vorhandene Dokumente
            int userCount = 0;
            while (countResult.next()) {
                userCount++;
            }

            if (userCount == 0) {
                // Keine Benutzer vorhanden - Standard-Benutzer aus config.json hinzufügen
                loadUsersFromConfigForMongo();
                System.out.println("Standard-Benutzer aus config.json wurden zur MongoDB users collection hinzugefügt.");
            } else {
                System.out.println("MongoDB users collection enthält bereits " + userCount + " Benutzer.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Initialisieren der MongoDB users collection: " + e.getMessage());
            // Fallback: Standard-Benutzer trotzdem hinzufügen
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
            // Tabelle erstellen falls sie nicht existiert
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
            // Standard-Benutzer aus config.json laden falls Tabelle leer ist
            countResult = databaseHandler.processor().query("SELECT COUNT(*) as count FROM users");
            if (countResult.next() && countResult.getInt("count") == 0) {
                // Benutzer aus config.json hinzufügen
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
            // Admin-Benutzer aus AppConfig hinzufügen
            AppConfig.AdminConfig admin = appConfig.getAdminConfig();
            addUserWithDetails(admin.getUsername(), admin.getPassword(), admin.getEmail(), admin.getRole());
            System.out.println("Admin-Benutzer aus Konfiguration hinzugefügt: " + admin.getUsername());

            // Standard-Benutzer aus AppConfig hinzufügen
            for (AppConfig.UserConfig user : appConfig.getDefaultUsers()) {
                addUserWithDetails(user.getUsername(), user.getPassword(), user.getEmail(), user.getRole());
                System.out.println("Standard-Benutzer aus Konfiguration hinzugefügt: " + user.getUsername());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Laden der Benutzer aus AppConfig: " + e.getMessage());
            // Fallback zu den ursprünglichen Standard-Benutzern
            addUser("admin", "admin");
            addUser("user", "user");
        }
    }

    private void loadUsersFromConfigForMongo() {
        try {
            // Admin-Benutzer aus AppConfig für MongoDB hinzufügen
            AppConfig.AdminConfig admin = appConfig.getAdminConfig();
            addUserToMongo(admin.getUsername(), admin.getPassword(), admin.getEmail(), admin.getRole(), admin.getPermissions());
            System.out.println("Admin-Benutzer zu MongoDB hinzugefügt: " + admin.getUsername());

            // Standard-Benutzer aus AppConfig für MongoDB hinzufügen
            for (AppConfig.UserConfig user : appConfig.getDefaultUsers()) {
                addUserToMongo(user.getUsername(), user.getPassword(), user.getEmail(), user.getRole(), user.getPermissions());
                System.out.println("Standard-Benutzer zu MongoDB hinzugefügt: " + user.getUsername());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fehler beim Laden der Benutzer in MongoDB: " + e.getMessage());
        }
    }

    public boolean addUserWithDetails(String username, String password, String email, String role) {
        // Passwort-Validierung basierend auf Security-Konfiguration
        if (password.length() < appConfig.getPasswordMinLength()) {
            System.err.println("Passwort zu kurz. Mindestlänge: " + appConfig.getPasswordMinLength());
            return false;
        }

        DatabaseResult checkResult = null;
        DatabaseResult insertResult = null;

        try {
            // Prüfen ob Benutzer bereits existiert
            checkResult = databaseHandler.processor().query(
                "SELECT COUNT(*) as count FROM users WHERE username = ?", username
            );

            if (checkResult.next() && checkResult.getInt("count") > 0) {
                System.out.println("Benutzer existiert bereits: " + username);
                return false; // Benutzer existiert bereits
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
            // Neuen Benutzer mit Details hinzufügen
            insertResult = databaseHandler.processor().update(
                "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)", 
                username, password, email, role
            );

            // Benutzerliste neu laden
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
