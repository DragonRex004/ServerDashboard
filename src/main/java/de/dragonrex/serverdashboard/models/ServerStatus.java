package de.dragonrex.serverdashboard.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ServerStatus {
    private int currentPlayers;
    private int maxPlayers;
    private String serverName;

    @Override
    public String toString() {
        return "ServerStatus{" +
                "currentPlayers=" + currentPlayers +
                ", maxPlayers=" + maxPlayers +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}
