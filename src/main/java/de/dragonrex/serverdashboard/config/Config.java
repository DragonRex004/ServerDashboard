package de.dragonrex.serverdashboard.config;

import org.json.JSONObject;

public record Config(JSONObject config) {

    /**
     * Fügt einen Standard-Wert zur Konfiguration hinzu, falls der Schlüssel noch nicht existiert
     * 
     * @param key Der Schlüssel für den Standard-Wert
     * @param defaultValue Der Standard-Wert, der hinzugefügt werden soll
     * @return true, wenn der Standard-Wert hinzugefügt wurde, false wenn der Schlüssel bereits existiert
     */
    public boolean addDefault(String key, Object defaultValue) {
        if (!config.has(key)) {
            config.put(key, defaultValue);
            return true;
        }
        return false;
    }

    /**
     * Fügt mehrere Standard-Werte zur Konfiguration hinzu
     * 
     * @param defaults JSONObject mit den Standard-Schlüssel-Wert-Paaren
     * @return Anzahl der hinzugefügten Standard-Werte
     */
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
