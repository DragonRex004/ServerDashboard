package de.dragonrex.controller;

import io.javalin.Javalin;

public class DashboardController {

    public DashboardController(Javalin app) {
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
    }
}
