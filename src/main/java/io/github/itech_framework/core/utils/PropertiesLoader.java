package io.github.itech_framework.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.exceptions.FrameworkException;

public class PropertiesLoader {
    public static final String PROFILE_PROPERTY = "flexi.profiles.active";
    public static final String PROFILE_ENV = "FLEXI_PROFILES_ACTIVE";
    public static final String DEFAULT_PROFILE = "dev";

    private static final String PROPERTIES_SUFFIX = ".properties";
    private static final Logger logger = LogManager.getLogger(PropertiesLoader.class);

    private static final Set<String> loadedBaseFiles = new LinkedHashSet<>();
    private static final Map<String, Properties> fileCache = new LinkedHashMap<>();
    private static volatile Properties properties = new Properties();
    private static volatile String activeProfile = DEFAULT_PROFILE;

    public static synchronized void load(String[] filenames, Class<?> clazz) {
        if (filenames == null) {
            throw new IllegalArgumentException("Properties file list must not be null");
        }

        ClassLoader classLoader = resolveClassLoader(clazz);

        for (String filename : filenames) {
            String normalized = normalize(filename);
            if (loadedBaseFiles.add(normalized)) {
                fileCache.put(normalized, readRequired(normalized, classLoader));
            }
        }

        Properties base = new Properties();
        for (String baseFile : loadedBaseFiles) {
            base.putAll(fileCache.get(baseFile));
        }

        String profile = resolveProfile(base);
        Properties merged = new Properties();
        merged.putAll(base);

        for (String baseFile : loadedBaseFiles) {
            String profileFile = getProfileFilename(baseFile, profile);
            if (profileFile == null) {
                continue;
            }
            Properties override = fileCache.get(profileFile);
            if (override == null) {
                override = readOptional(profileFile, classLoader);
                if (override != null) {
                    fileCache.put(profileFile, override);
                }
            }
            if (override != null) {
                merged.putAll(override);
            } else {
                logger.debug("Profile override not found: {}", profileFile);
            }
        }

        activeProfile = profile;
        properties = merged;
        logger.info("Active profile: {}", profile);
    }

    public static void loadWithProfile(String baseName) {
        load(new String[] { baseName }, null);
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private static String resolveProfile(Properties base) {
        String fromSystem = clean(System.getProperty(PROFILE_PROPERTY));
        if (fromSystem != null) {
            return fromSystem;
        }
        String fromEnv = clean(System.getenv(PROFILE_ENV));
        if (fromEnv != null) {
            return fromEnv;
        }
        String fromFile = clean(base.getProperty(PROFILE_PROPERTY));
        if (fromFile != null) {
            return fromFile;
        }
        return DEFAULT_PROFILE;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalize(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Properties file name must not be null or blank");
        }
        String trimmed = filename.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private static String getProfileFilename(String baseName, String profile) {
        if (!baseName.endsWith(PROPERTIES_SUFFIX)) {
            return null;
        }
        String base = baseName.substring(0, baseName.length() - PROPERTIES_SUFFIX.length());
        return base + "-" + profile + PROPERTIES_SUFFIX;
    }

    private static ClassLoader resolveClassLoader(Class<?> clazz) {
        if (clazz != null && clazz.getClassLoader() != null) {
            return clazz.getClassLoader();
        }
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            return context;
        }
        ClassLoader own = PropertiesLoader.class.getClassLoader();
        if (own != null) {
            return own;
        }
        return ClassLoader.getSystemClassLoader();
    }

    private static Properties readRequired(String filename, ClassLoader classLoader) {
        Properties loaded = readOptional(filename, classLoader);
        if (loaded == null) {
            throw new FrameworkException("Required properties file not found: " + filename);
        }
        return loaded;
    }

    private static Properties readOptional(String filename, ClassLoader classLoader) {
        try (InputStream input = classLoader.getResourceAsStream(filename)) {
            if (input == null) {
                return null;
            }
            Properties loaded = new Properties();
            loaded.load(input);
            logger.debug("Loaded properties file {} ({} entries)", filename, loaded.size());
            return loaded;
        } catch (IOException | IllegalArgumentException e) {
            throw new FrameworkException("Error loading properties file: " + filename, e);
        }
    }
}