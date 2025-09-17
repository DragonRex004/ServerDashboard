package de.dragonrex.serverdashboard.config;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    
    public void initializeConfig(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create config", e);
            }
        }
    }

    public Config loadConfig(Path path) {
        try {
            String json = new String(Files.readAllBytes(path));
            return new Config(new JSONObject(json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }
}
