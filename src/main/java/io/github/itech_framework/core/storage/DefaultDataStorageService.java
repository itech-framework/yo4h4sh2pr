package io.github.itech_framework.core.storage;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.github.itech_framework.core.exceptions.DataStorageException;

public class DefaultDataStorageService implements DataStorageService {
    private static final Logger logger = LogManager.getLogger(DefaultDataStorageService.class);

    private final Preferences prefs;
    private final Gson gson;

    public DefaultDataStorageService(Class<?> appClass) {
        this.prefs = Preferences.userNodeForPackage(appClass);
        this.gson = new Gson();
        logger.debug("Data storage initialized for node '{}'", prefs.absolutePath());
    }

    @Override
    public String load(String key) {
        checkKey(key);
        String value = prefs.get(key, null);
        if (value == null) {
            logger.debug("No value found for key '{}'", key);
        } else {
            logger.debug("Loaded value for key '{}' ({} characters)", key, value.length());
        }
        return value;
    }

    @Override
    public void save(String key, String value) {
        checkKey(key);
        if (value == null) {
            prefs.remove(key);
            logger.debug("Removed key '{}'", key);
        } else {
            checkValue(key, value);
            prefs.put(key, value);
            logger.debug("Saved value for key '{}' ({} characters)", key, value.length());
        }
        flush(key);
    }

    @Override
    public <T> T load(String key, Class<T> type) {
        checkKey(key);
        String json = prefs.get(key, null);
        if (json == null) {
            logger.debug("No value found for key '{}' (requested type {})", key, type.getName());
            return null;
        }
        try {
            T result = gson.fromJson(json, type);
            logger.debug("Loaded key '{}' as {} ({} characters)", key, type.getName(), json.length());
            return result;
        } catch (JsonParseException e) {
            logger.error("Stored value for key '{}' could not be deserialized as {}", key, type.getName(), e);
            throw new DataStorageException("Stored value for key '" + key
                    + "' could not be deserialized as " + type.getName(), e);
        }
    }

    @Override
    public void save(String key, Object value) {
        checkKey(key);
        if (value == null) {
            prefs.remove(key);
            logger.debug("Removed key '{}'", key);
        } else {
            String json;
            try {
                json = gson.toJson(value);
            } catch (JsonParseException | StackOverflowError e) {
                logger.error("Could not serialize value of type {} for key '{}'", value.getClass().getName(), key, e);
                throw new DataStorageException("Could not serialize value for key '" + key + "'", e);
            }
            checkValue(key, json);
            prefs.put(key, json);
            logger.debug("Saved key '{}' as {} ({} characters)", key, value.getClass().getName(), json.length());
        }
        flush(key);
    }

    private void checkKey(String key) {
        if (key == null || key.isEmpty()) {
            logger.error("Rejected storage operation: key is null or empty");
            throw new IllegalArgumentException("Storage key must not be null or empty");
        }
        if (key.length() > Preferences.MAX_KEY_LENGTH) {
            logger.error("Rejected key '{}': {} characters exceeds the limit of {}",
                    key, key.length(), Preferences.MAX_KEY_LENGTH);
            throw new DataStorageException("Key '" + key + "' exceeds " + Preferences.MAX_KEY_LENGTH + " characters");
        }
    }

    private void checkValue(String key, String value) {
        if (value.length() > Preferences.MAX_VALUE_LENGTH) {
            logger.error("Rejected value for key '{}': {} characters exceeds the limit of {}. Nothing was saved.",
                    key, value.length(), Preferences.MAX_VALUE_LENGTH);
            throw new DataStorageException("Value for key '" + key + "' is " + value.length()
                    + " characters; Preferences allows at most " + Preferences.MAX_VALUE_LENGTH
                    + ". Nothing was saved.");
        }
    }

    private void flush(String key) {
        try {
            prefs.flush();
            logger.trace("Flushed preferences after change to key '{}'", key);
        } catch (BackingStoreException e) {
            logger.error("Could not persist key '{}' to the backing store", key, e);
            throw new DataStorageException("Could not persist key '" + key + "' to the backing store", e);
        }
    }
}