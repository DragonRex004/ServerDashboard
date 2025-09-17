package de.dragonrex.serverdashboard.controller;

import de.dragonrex.serverdashboard.config.AppConfig;
import de.dragonrex.serverdashboard.user.UserManager;
import io.javalin.Javalin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    private final UserManager userManager;
    private final AppConfig appConfig;
    private final Map<String, Integer> loginAttempts = new HashMap<>();

    public LoginController(Javalin app, UserManager userManager, AppConfig appConfig) {
        this.userManager = userManager;
        this.appConfig = appConfig;

        app.post("/api/login", ctx -> {
            LOGGER.info("Login-Versuch von: " + ctx.ip());
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            String clientIp = ctx.ip();

            // Unterstütze sowohl JSON- als auch Form-Parameter
            String contentType = ctx.header("Content-Type");
            LOGGER.info("Content-Type: " + contentType);
            LOGGER.info("Body: " + ctx.body());

            // Prüfe maximale Login-Versuche
            if (isBlocked(clientIp)) {
                ctx.status(429);
                ctx.result("Zu viele Login-Versuche. Bitte versuchen Sie es später erneut.");
                LOGGER.warning("Login blockiert für IP: " + clientIp + " (zu viele Versuche)");
                return;
            }

            if (isValidUser(username, password)) {
                // Erfolgreiche Anmeldung - Versuche zurücksetzen
                resetLoginAttempts(clientIp);

                ctx.sessionAttribute("username", username);
                ctx.sessionAttribute("loginTime", System.currentTimeMillis());

                // Benutzerrolle basierend auf Authentifizierungsquelle setzen
                String userRole = getUserRole(username.trim());
                ctx.sessionAttribute("userRole", userRole);
                LOGGER.info("Benutzerrolle für " + username + ": " + userRole);

                ctx.status(200);

                // Für JSON-Requests JSON-Response senden, sonst Redirect
                if (contentType != null && contentType.contains("application/json")) {
                    ctx.json(Map.of("success", true, "message", "Erfolgreich angemeldet", "redirect", "/dashboard.html"));
                } else {
                    ctx.redirect("/dashboard.html");
                }

                LOGGER.info("Erfolgreiche Anmeldung: " + username + " von IP: " + clientIp);
            } else {
                // Fehlgeschlagene Anmeldung - Versuche zählen
                incrementLoginAttempts(clientIp);

                ctx.status(401);

                // Für JSON-Requests JSON-Response senden, sonst Text
                if (contentType != null && contentType.contains("application/json")) {
                    ctx.json(Map.of("success", false, "message", "Ungültiger Benutzername oder Passwort."));
                } else {
                    ctx.result("Ungültiger Benutzername oder Passwort.");
                }

                LOGGER.warning("Fehlgeschlagene Anmeldung für: " + username + " von IP: " + clientIp);
            }
        });

        app.post("/api/logout", ctx -> {
            String username = ctx.sessionAttribute("username");
            ctx.sessionAttributeMap().clear();
            ctx.redirect("/");

            if (username != null) {
                LOGGER.info("Benutzer abgemeldet: " + username);
            }
        });

        // Session-Validation-Endpoint
        app.get("/api/session/validate", ctx -> {
            String username = ctx.sessionAttribute("username");
            Long loginTime = ctx.sessionAttribute("loginTime");

            if (username != null && loginTime != null) {
                long sessionAge = System.currentTimeMillis() - loginTime;
                long maxSessionAge = appConfig.getSessionTimeout() * 1000L;

                if (sessionAge < maxSessionAge) {
                    String userRole = ctx.sessionAttribute("userRole");
                    if (userRole == null) {
                        userRole = getUserRole(username);
                        ctx.sessionAttribute("userRole", userRole);
                    }

                    ctx.json(Map.of(
                        "valid", true,
                        "username", username,
                        "role", userRole,
                        "permissions", getUserPermissions(username),
                        "remainingTime", (maxSessionAge - sessionAge) / 1000
                    ));
                } else {
                    ctx.sessionAttributeMap().clear();
                    ctx.json(Map.of("valid", false, "reason", "Session abgelaufen"));
                }
            } else {
                ctx.json(Map.of("valid", false, "reason", "Keine aktive Session"));
            }
        });
    }

    private boolean isValidUser(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return false;
        }

        // Passwort-Mindestlänge prüfen
        if (password.length() < appConfig.getPasswordMinLength()) {
            LOGGER.info("Passwort zu kurz für Benutzer: " + username);
            return false;
        }

        String trimmedUsername = username.trim();

        // Zuerst gegen Standard-Admin aus config.json prüfen
        AppConfig.AdminConfig adminConfig = appConfig.getAdminConfig();
        if (adminConfig.getUsername().equals(trimmedUsername) && 
            adminConfig.getPassword().equals(password)) {
            LOGGER.info("Erfolgreiche Authentifizierung gegen Standard-Admin aus config.json: " + trimmedUsername);
            return true;
        }

        // Dann gegen Standard-Benutzer aus config.json prüfen
        for (AppConfig.UserConfig userConfig : appConfig.getDefaultUsers()) {
            if (userConfig.getUsername().equals(trimmedUsername) && 
                userConfig.getPassword().equals(password)) {
                LOGGER.info("Erfolgreiche Authentifizierung gegen Standard-Benutzer aus config.json: " + trimmedUsername);
                return true;
            }
        }

        // Fallback zur datenbankbasierten Authentifizierung
        boolean dbAuth = this.userManager.authenticateUser(trimmedUsername, password);
        if (dbAuth) {
            LOGGER.info("Erfolgreiche Authentifizierung gegen Datenbank: " + trimmedUsername);
        } else {
            LOGGER.info("Authentifizierung fehlgeschlagen für alle Methoden: " + trimmedUsername);
        }

        return dbAuth;
    }

    private boolean isBlocked(String clientIp) {
        Integer attempts = loginAttempts.get(clientIp);
        return attempts != null && attempts >= appConfig.getMaxLoginAttempts();
    }

    private void incrementLoginAttempts(String clientIp) {
        loginAttempts.merge(clientIp, 1, Integer::sum);
    }

    private void resetLoginAttempts(String clientIp) {
        loginAttempts.remove(clientIp);
    }

    /**
     * Ermittelt die Benutzerrolle basierend auf der Authentifizierungsquelle
     */
    private String getUserRole(String username) {
        // Prüfe Admin aus config.json
        AppConfig.AdminConfig adminConfig = appConfig.getAdminConfig();
        if (adminConfig.getUsername().equals(username)) {
            return adminConfig.getRole();
        }

        // Prüfe Standard-Benutzer aus config.json
        for (AppConfig.UserConfig userConfig : appConfig.getDefaultUsers()) {
            if (userConfig.getUsername().equals(username)) {
                return userConfig.getRole();
            }
        }

        // Fallback für Datenbankbenutzer - Standard-Rolle
        return "user";
    }

    /**
     * Gibt die Berechtigungen für den aktuellen Benutzer zurück
     */
    private java.util.List<String> getUserPermissions(String username) {
        // Prüfe Admin aus config.json
        AppConfig.AdminConfig adminConfig = appConfig.getAdminConfig();
        if (adminConfig.getUsername().equals(username)) {
            return adminConfig.getPermissions();
        }

        // Prüfe Standard-Benutzer aus config.json
        for (AppConfig.UserConfig userConfig : appConfig.getDefaultUsers()) {
            if (userConfig.getUsername().equals(username)) {
                return userConfig.getPermissions();
            }
        }

        // Fallback für Datenbankbenutzer - Standard-Berechtigungen
        return java.util.List.of("dashboard_access");
    }
}
