package io.github.itech_framework.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesLoader {
    private static final Properties properties = new Properties();
    private static final List<String> loadedBaseFiles = new ArrayList<>();
    private static String activeProfile = null;

    public static void load(String[] filenames, Class<?> clazz) {
        if (filenames == null) return;

        Properties tempProps = new Properties();
        for (String filename : filenames) {
            loadSingleInto(filename, tempProps, true);
            loadedBaseFiles.add(filename);
        }

        String resolvedProfile = System.getProperty("flexi.profiles.active");
        if (resolvedProfile == null) {
            resolvedProfile = tempProps.getProperty("flexi.profiles.active");
        }
        if (resolvedProfile == null) {
            resolvedProfile = "dev";
        }
        activeProfile = resolvedProfile;
        System.out.println("Active profile: " + activeProfile);

        for (String filename : loadedBaseFiles) {
            loadSingleInto(filename, properties, true);
        }

        for (String baseName : loadedBaseFiles) {
            String profileName = getProfileFilename(baseName);
            if (profileName != null) {
                loadSingleInto(profileName, properties, false);
            }
        }
    }

    public static void loadWithProfile(String baseName) {
        load(new String[]{baseName}, null);
    }

    private static String getProfileFilename(String baseName) {
        if (baseName == null || !baseName.endsWith(".properties")) {
            return null;
        }
        String base = baseName.substring(0, baseName.length() - 11);
        return base + "-" + activeProfile + ".properties";
    }

    private static void loadSingleInto(String filename, Properties target, boolean required) {
        try (InputStream input = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream(filename)) {
            if (input == null) {
                if (required) {
                    throw new RuntimeException("Required properties file not found: " + filename);
                } else {
                    System.out.println("Profile override not found: " + filename);
                    return;
                }
            }
            target.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading properties file: " + filename, e);
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}