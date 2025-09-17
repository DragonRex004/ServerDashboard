package de.dragonrex.serverdashboard.controller;

import de.dragonrex.serverdashboard.config.AppConfig;
import io.javalin.Javalin;

import java.util.logging.Logger;

public class DashboardController {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    private final AppConfig appConfig;

    public DashboardController(Javalin app, AppConfig appConfig) {
        this.appConfig = appConfig;

        app.before("/dashboard.html", ctx -> {
            if (ctx.sessionAttribute("username") == null) {
                ctx.redirect("/");
            }
        });

        app.get("/dashboard", ctx -> {
            String user = ctx.sessionAttribute("username");
            if (user == null) {
                ctx.redirect("/");
            } else {
                ctx.redirect("/dashboard.html");
            }
        });

        // Dashboard.html wird automatisch als statische Datei bereitgestellt
        // Nur Session-Validierung durchführen
        app.before("/dashboard.html", ctx -> {
            String username = ctx.sessionAttribute("username");
            if (username == null) {
                ctx.redirect("/");
                return;
            }

            // Session-Validierung
            Long loginTime = ctx.sessionAttribute("loginTime");
            if (loginTime != null) {
                long sessionAge = System.currentTimeMillis() - loginTime;
                if (sessionAge > appConfig.getSessionTimeout() * 1000L) {
                    ctx.sessionAttributeMap().clear();
                    ctx.redirect("/?expired=true");
                    return;
                }
            }

            LOGGER.info("Dashboard angezeigt für Benutzer: " + username);
        });

        // Dashboard-API für dynamische Inhalte
        app.get("/api/dashboard/info", ctx -> {
            String username = ctx.sessionAttribute("username");
            if (username == null) {
                ctx.status(401);
                return;
            }

            ctx.json(java.util.Map.of(
                "application", java.util.Map.of(
                    "name", appConfig.getApplicationName(),
                    "version", appConfig.getApplicationVersion(),
                    "environment", appConfig.getEnvironment()
                ),
                "user", java.util.Map.of(
                    "username", username,
                    "sessionTimeout", appConfig.getSessionTimeout()
                ),
                "features", java.util.Map.of(
                    "userRegistration", appConfig.isUserRegistration(),
                    "passwordReset", appConfig.isPasswordReset(),
                    "twoFactorAuth", appConfig.isTwoFactorAuth()
                )
            ));
        });
    }

}
