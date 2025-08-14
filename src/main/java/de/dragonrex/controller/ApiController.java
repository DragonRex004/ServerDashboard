package de.dragonrex.controller;

import io.javalin.Javalin;

import java.util.Map;

public class ApiController {

    public ApiController(Javalin app) {

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
    }
}
