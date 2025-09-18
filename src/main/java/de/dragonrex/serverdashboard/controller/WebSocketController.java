package de.dragonrex.serverdashboard.controller;

import de.dragonrex.serverdashboard.models.ServerStatus;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class WebSocketController {
    private ServerStatus serverStatus = new ServerStatus();

    public WebSocketController(Javalin app) {

        // TEST-DATA
        this.serverStatus.setServerName("GMod-Server");
        this.serverStatus.setCurrentPlayers(5);
        this.serverStatus.setMaxPlayers(30);

        app.ws("/ws/serverstatus", ws -> {
            ws.onConnect(ctx -> System.out.println("Gmod Server-Connection established."));
            ws.onMessage(ctx -> {
                ServerStatus newStatus = ctx.messageAsClass(ServerStatus.class);
                serverStatus = newStatus;
                System.out.println("WebSocket-Update received: " + newStatus.toString());


                app.ws("/ws/dashboard", dashboardCtx -> {
                    if (ctx.sessionAttribute("username") != null) {
                        ctx.send(ctx.message());
                    }
                });
            });
            ws.onClose(ctx -> System.out.println("WebSocket-Connection to the Gmod Server was closed."));
            ws.onError(ctx -> System.err.println("WebSocket-Error: " + ctx.error()));
        });

        app.ws("/ws/dashboard", ws -> {
            ws.onConnect(ctx -> {
                System.out.println("Dashboard-Client connected.");
                ctx.send(JavalinJackson.defaultMapper().writeValueAsString(serverStatus));
            });
        });
    }
}
