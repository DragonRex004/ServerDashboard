package de.dragonrex.serverdashboard.config;

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AppConfig {
    // Application Settings
    private String applicationName;
    private String applicationVersion;
    private int applicationPort;
    private String environment;

    // Security Settings
    private int sessionTimeout;
    private int maxLoginAttempts;
    private int passwordMinLength;

    // Admin Configuration
    private AdminConfig adminConfig;

    // Default Users
    private List<UserConfig> defaultUsers;

    // Database Settings
    private boolean autoMigrate;
    private boolean backupEnabled;
    private int backupIntervalHours;

    // Logging Settings
    private String loggingLevel;
    private boolean fileEnabled;
    private boolean consoleEnabled;

    // Features
    private boolean userRegistration;
    private boolean passwordReset;
    private boolean twoFactorAuth;

    private final JSONObject configJson;

    public AppConfig(String configPath) {
        this.defaultUsers = new ArrayList<>();
        this.configJson = loadConfigFromFile(configPath);
        parseConfig();
    }

    private JSONObject loadConfigFromFile(String configPath) {
        try {
            String content = new String(Files.readAllBytes(Path.of(configPath)));
            return new JSONObject(content);
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der config.json: " + e.getMessage());
            // Fallback zu Standard-Konfiguration
            return createDefaultConfig();
        }
    }

    private JSONObject createDefaultConfig() {
        JSONObject defaultConfig = new JSONObject();

        // Application defaults
        JSONObject app = new JSONObject();
        app.put("name", "Server Dashboard");
        app.put("version", "1.0.0");
        app.put("port", 7070);
        app.put("environment", "development");
        defaultConfig.put("application", app);

        // Security defaults
        JSONObject security = new JSONObject();
        security.put("session_timeout", 3600);
        security.put("max_login_attempts", 5);
        security.put("password_min_length", 4);
        defaultConfig.put("security", security);

        // Admin defaults
        JSONObject admin = new JSONObject();
        admin.put("username", "admin");
        admin.put("password", "admin123");
        admin.put("email", "admin@serverdashboard.local");
        admin.put("role", "administrator");
        defaultConfig.put("admin", admin);

        return defaultConfig;
    }

    private void parseConfig() {
        // Parse Application Settings
        JSONObject app = configJson.optJSONObject("application");
        if (app != null) {
            this.applicationName = app.optString("name", "Server Dashboard");
            this.applicationVersion = app.optString("version", "1.0.0");
            this.applicationPort = app.optInt("port", 7070);
            this.environment = app.optString("environment", "development");
        } else {
            setApplicationDefaults();
        }

        // Parse Security Settings
        JSONObject security = configJson.optJSONObject("security");
        if (security != null) {
            this.sessionTimeout = security.optInt("session_timeout", 3600);
            this.maxLoginAttempts = security.optInt("max_login_attempts", 5);
            this.passwordMinLength = security.optInt("password_min_length", 4);
        } else {
            setSecurityDefaults();
        }

        // Parse Admin Configuration
        JSONObject admin = configJson.optJSONObject("admin");
        if (admin != null) {
            this.adminConfig = new AdminConfig(
                admin.optString("username", "admin"),
                admin.optString("password", "admin123"),
                admin.optString("email", "admin@serverdashboard.local"),
                admin.optString("role", "administrator"),
                parseStringArray(admin.optJSONArray("permissions"))
            );
        } else {
            this.adminConfig = new AdminConfig("admin", "admin123", "admin@serverdashboard.local", "administrator", List.of("user_management", "system_configuration"));
        }

        // Parse Default Users
        JSONArray defaultUsersArray = configJson.optJSONArray("default_users");
        if (defaultUsersArray != null) {
            for (int i = 0; i < defaultUsersArray.length(); i++) {
                JSONObject user = defaultUsersArray.getJSONObject(i);
                this.defaultUsers.add(new UserConfig(
                    user.optString("username", "user"),
                    user.optString("password", "user123"),
                    user.optString("email", "user@serverdashboard.local"),
                    user.optString("role", "user"),
                    parseStringArray(user.optJSONArray("permissions"))
                ));
            }
        }

        // Parse Database Settings
        JSONObject database = configJson.optJSONObject("database");
        if (database != null) {
            this.autoMigrate = database.optBoolean("auto_migrate", true);
            this.backupEnabled = database.optBoolean("backup_enabled", true);
            this.backupIntervalHours = database.optInt("backup_interval_hours", 24);
        } else {
            setDatabaseDefaults();
        }

        // Parse Logging Settings
        JSONObject logging = configJson.optJSONObject("logging");
        if (logging != null) {
            this.loggingLevel = logging.optString("level", "INFO");
            this.fileEnabled = logging.optBoolean("file_enabled", true);
            this.consoleEnabled = logging.optBoolean("console_enabled", true);
        } else {
            setLoggingDefaults();
        }

        // Parse Features
        JSONObject features = configJson.optJSONObject("features");
        if (features != null) {
            this.userRegistration = features.optBoolean("user_registration", false);
            this.passwordReset = features.optBoolean("password_reset", true);
            this.twoFactorAuth = features.optBoolean("two_factor_auth", false);
        } else {
            setFeatureDefaults();
        }
    }

    private List<String> parseStringArray(JSONArray array) {
        List<String> result = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        }
        return result;
    }

    private void setApplicationDefaults() {
        this.applicationName = "Server Dashboard";
        this.applicationVersion = "1.0.0";
        this.applicationPort = 7070;
        this.environment = "development";
    }

    private void setSecurityDefaults() {
        this.sessionTimeout = 3600;
        this.maxLoginAttempts = 5;
        this.passwordMinLength = 4;
    }

    private void setDatabaseDefaults() {
        this.autoMigrate = true;
        this.backupEnabled = true;
        this.backupIntervalHours = 24;
    }

    private void setLoggingDefaults() {
        this.loggingLevel = "INFO";
        this.fileEnabled = true;
        this.consoleEnabled = true;
    }

    private void setFeatureDefaults() {
        this.userRegistration = false;
        this.passwordReset = true;
        this.twoFactorAuth = false;
    }

    // Inner Classes
    @Getter
    public static class AdminConfig {
        private final String username;
        private final String password;
        private final String email;
        private final String role;
        private final List<String> permissions;

        public AdminConfig(String username, String password, String email, String role, List<String> permissions) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
            this.permissions = permissions != null ? permissions : new ArrayList<>();
        }
    }

    @Getter
    public static class UserConfig {
        private final String username;
        private final String password;
        private final String email;
        private final String role;
        private final List<String> permissions;

        public UserConfig(String username, String password, String email, String role, List<String> permissions) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.role = role;
            this.permissions = permissions != null ? permissions : new ArrayList<>();
        }
    }

    public boolean isDevelopment() {
        return "development".equalsIgnoreCase(this.environment);
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(this.environment);
    }
}
