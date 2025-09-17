package de.dragonrex.serverdashboard.config;

import org.json.JSONObject;

public record Config(JSONObject config) {

    public boolean addDefault(String key, Object defaultValue) {
        if (!config.has(key)) {
            config.put(key, defaultValue);
            return true;
        }
        return false;
    }

    public int addDefaults(JSONObject defaults) {
        int added = 0;
        for (String key : defaults.keySet()) {
            if (addDefault(key, defaults.get(key))) {
                added++;
            }
        }
        return added;
    }
}
