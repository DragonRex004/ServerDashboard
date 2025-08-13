package de.dragonrex;

import de.dragonrex.user.UserManager;
import io.javalin.Javalin;

public class ServerDashboard {
    private UserManager userManager;

    public ServerDashboard() {
        this.userManager = new UserManager("config/users.json");
        this.userManager.loadUser();
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(7070);

        app.post("/api/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            if (isValidUser(username, password)) {
                ctx.status(200);
                ctx.redirect("/dashboard.html");
            } else {
                ctx.status(401);
                ctx.result("Ungültige Anmeldedaten.");
            }
        });
        app.get("/dashboard", ctx -> ctx.redirect("/dashboard.html"));
        app.get("/api/server/status", ctx -> {
            ctx.json("{\"status\" : \"16/32\"}");
        });
    }

    public static void main(String[] args) {
        new ServerDashboard();
    }

    private boolean isValidUser(String username, String password) {
        if (this.userManager.getUserList() == null) {
            return false;
        }
        return this.userManager.getUserList().stream()
                .anyMatch(user -> user.getName().equals(username) && user.getPassword().equals(password));
    }
}
