package de.dragonrex.controller;

import de.dragonrex.user.UserManager;
import io.javalin.Javalin;

public class LoginController {
    private UserManager userManager;

    public LoginController(Javalin app, UserManager userManager) {
        this.userManager = userManager;
        app.post("/api/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            if (isValidUser(username, password)) {
                ctx.sessionAttribute("username", username);
                ctx.status(200);
                ctx.redirect("/dashboard.html");
            } else {
                ctx.status(401);
                ctx.result("invalid username or password.");
            }
        });

        app.post("/api/logout", ctx -> {
            ctx.sessionAttributeMap().clear();
            ctx.redirect("/");
        });
    }

    private boolean isValidUser(String username, String password) {
        if (this.userManager.getUserList() == null) {
            return false;
        }
        return this.userManager.getUserList().stream()
                .anyMatch(user -> user.getName().equals(username) && user.getPassword().equals(password));
    }
}
