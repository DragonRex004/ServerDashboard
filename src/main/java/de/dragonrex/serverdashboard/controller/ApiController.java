package de.dragonrex.serverdashboard.controller;

import de.dragonrex.serverdashboard.config.AppConfig;
import io.javalin.Javalin;

import java.util.Map;
import java.util.logging.Logger;

public class ApiController {
    private static final Logger LOGGER = Logger.getLogger(ApiController.class.getName());

    private final AppConfig appConfig;

    public ApiController(Javalin app, AppConfig appConfig) {
        this.appConfig = appConfig;

        app.get("/api/user/me", ctx -> {
            String username = ctx.sessionAttribute("username");
            if (username != null) {
                Map<String, String> userMap = Map.of("username", username);
                ctx.json(userMap);
            } else {
                ctx.status(401);
                ctx.result("Nicht eingeloggt.");
            }
        });

        app.get("/api/server/status", ctx -> {
            ctx.json("{\"status\" : \"16/32\"}");
        });

        // Test-Endpoint
        app.get("/api/test", ctx -> {
            ctx.json(Map.of(
                "status", "OK",
                "message", "API ist funktionsfähig",
                "timestamp", System.currentTimeMillis(),
                "application", appConfig.getApplicationName(),
                "version", appConfig.getApplicationVersion()
            ));
        });

        // Öffentliche App-Informationen
        app.get("/api/info", ctx -> {
            ctx.json(Map.of(
                "application", Map.of(
                    "name", appConfig.getApplicationName(),
                    "version", appConfig.getApplicationVersion(),
                    "environment", appConfig.getEnvironment()
                ),
                "features", Map.of(
                    "userRegistration", appConfig.isUserRegistration(),
                    "passwordReset", appConfig.isPasswordReset(),
                    "twoFactorAuth", appConfig.isTwoFactorAuth()
                ),
                "security", Map.of(
                    "passwordMinLength", appConfig.getPasswordMinLength(),
                    "maxLoginAttempts", appConfig.getMaxLoginAttempts()
                )
            ));
        });

        // Gesicherte API-Endpoints (erfordern Authentifizierung)
        app.get("/api/user/profile", ctx -> {
            String username = ctx.sessionAttribute("username");
            if (username == null) {
                ctx.status(401).json(Map.of("error", "Nicht authentifiziert"));
                return;
            }

            ctx.json(Map.of(
                "username", username,
                "sessionTimeout", appConfig.getSessionTimeout(),
                "loginTime", ctx.sessionAttribute("loginTime")
            ));
        });

        // Health Check Endpoint
        app.get("/api/health", ctx -> {
            ctx.json(Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis(),
                "uptime", System.currentTimeMillis() - getStartTime(),
                "environment", appConfig.getEnvironment()
            ));
        });
    }

    private long startTime = System.currentTimeMillis();

    private long getStartTime() {
        return startTime;
    }
}
