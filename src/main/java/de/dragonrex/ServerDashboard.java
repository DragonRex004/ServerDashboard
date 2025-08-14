package de.dragonrex;

import de.dragonrex.controller.ApiController;
import de.dragonrex.controller.DashboardController;
import de.dragonrex.controller.LoginController;
import de.dragonrex.controller.WebSocketController;
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

        new LoginController(app, this.userManager);
        new DashboardController(app);
        new ApiController(app);
        new WebSocketController(app);
    }

    public static void main(String[] args) {
        new ServerDashboard();
    }
}
