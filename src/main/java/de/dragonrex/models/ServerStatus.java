package de.dragonrex.models;

public class ServerStatus {
    private int currentPlayers;
    private int maxPlayers;
    private String serverName;

    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    @Override
    public String toString() {
        return "ServerStatus{" +
                "currentPlayers=" + currentPlayers +
                ", maxPlayers=" + maxPlayers +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}
