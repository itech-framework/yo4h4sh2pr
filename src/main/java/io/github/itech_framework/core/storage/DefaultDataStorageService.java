package io.github.itech_framework.core.storage;
import java.util.prefs.Preferences;

import com.google.gson.Gson;

public class DefaultDataStorageService implements DataStorageService{
    private final Preferences prefs;
    private final Gson gson;

    public DefaultDataStorageService() {
        prefs = Preferences.userNodeForPackage(DefaultDataStorageService.class);
        gson = new Gson();
    }

    @Override
    public String load(String key) {
        return prefs.get(key, null);
    }

    @Override
    public void save(String key, String value) {
        if (value == null) {
            prefs.remove(key);
        } else {
            prefs.put(key, value);
        }
    }
    
    @Override
    public <T> T load(String key, Class<T> type) {
        String json = prefs.get(key, null);
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            System.err.println("Error deserializing object for key: " + key);
            return null;
        }
    }

    @Override
    public void save(String key, Object value) {
        if (value == null) {
            prefs.remove(key);
        } else {
            try {
                String json = gson.toJson(value);
                prefs.put(key, json);
            } catch (Exception e) {
                System.err.println("Error serializing object for key: " + key);
            }
        }
    }
}
