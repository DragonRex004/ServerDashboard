package de.dragonrex.serverdashboard.controller;

import de.dragonrex.serverdashboard.config.AppConfig;
import de.dragonrex.serverdashboard.user.UserManager;
import io.javalin.Javalin;

import java.util.HashMap;
import java.util.List;
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

            String contentType = ctx.header("Content-Type");
            LOGGER.info("Content-Type: " + contentType);
            LOGGER.info("Body: " + ctx.body());

            if (isBlocked(clientIp)) {
                ctx.status(429);
                ctx.result("Zu viele Login-Versuche. Bitte versuchen Sie es später erneut.");
                LOGGER.warning("Login blockiert für IP: " + clientIp + " (zu viele Versuche)");
                return;
            }

            if (isValidUser(username, password)) {
                resetLoginAttempts(clientIp);

                ctx.sessionAttribute("username", username);
                ctx.sessionAttribute("loginTime", System.currentTimeMillis());

                String userRole = getUserRole(username.trim());
                ctx.sessionAttribute("userRole", userRole);
                LOGGER.info("Benutzerrolle für " + username + ": " + userRole);

                ctx.status(200);

                if (contentType != null && contentType.contains("application/json")) {
                    ctx.json(Map.of("success", true, "message", "Erfolgreich angemeldet", "redirect", "/dashboard.html"));
                } else {
                    ctx.redirect("/dashboard.html");
                }

                LOGGER.info("Erfolgreiche Anmeldung: " + username + " von IP: " + clientIp);
            } else {
                incrementLoginAttempts(clientIp);

                ctx.status(401);

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

        if (password.length() < appConfig.getPasswordMinLength()) {
            LOGGER.info("Passwort zu kurz für Benutzer: " + username);
            return false;
        }

        String trimmedUsername = username.trim();

        AppConfig.AdminConfig adminConfig = appConfig.getAdminConfig();
        if (adminConfig.username().equals(trimmedUsername) &&
            adminConfig.password().equals(password)) {
            LOGGER.info("Erfolgreiche Authentifizierung gegen Standard-Admin aus config.json: " + trimmedUsername);
            return true;
        }

        for (AppConfig.UserConfig userConfig : appConfig.getDefaultUsers()) {
            if (userConfig.username().equals(trimmedUsername) &&
                userConfig.password().equals(password)) {
                LOGGER.info("Erfolgreiche Authentifizierung gegen Standard-Benutzer aus config.json: " + trimmedUsername);
                return true;
            }
        }

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

    private String getUserRole(String username) {
        AppConfig.AdminConfig adminConfig = appConfig.getAdminConfig();
        if (adminConfig.username().equals(username)) {
            return adminConfig.role();
        }

        for (AppConfig.UserConfig userConfig : appConfig.getDefaultUsers()) {
            if (userConfig.username().equals(username)) {
                return userConfig.role();
            }
        }

        return "user";
    }

    private java.util.List<String> getUserPermissions(String username) {
        AppConfig.AdminConfig adminConfig = appConfig.getAdminConfig();
        if (adminConfig.username().equals(username)) {
            return adminConfig.permissions();
        }

        for (AppConfig.UserConfig userConfig : appConfig.getDefaultUsers()) {
            if (userConfig.username().equals(username)) {
                return userConfig.permissions();
            }
        }

        return List.of("dashboard_access");
    }
}
