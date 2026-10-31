package io.github.itech_framework.core.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.itech_framework.core.exceptions.DataStorageException;

class DefaultDataStorageServiceTest {

    enum Mode {
        LIGHT, DARK
    }

    static class Settings {
        String theme;
        int size;
    }

    private Preferences root;
    private Preferences current;
    private Preferences legacy;
    private DefaultDataStorageService service;

    @BeforeEach
    void createNodes() {
        root = Preferences.userRoot().node("itech-framework-tests/" + UUID.randomUUID());
        current = root.node("current");
        legacy = root.node("legacy");
        service = new DefaultDataStorageService(current, null);
    }

    @AfterEach
    void removeNodes() throws BackingStoreException {
        root.removeNode();
    }

    @Test
    void storesStringsRawAndLoadsThemBackWithoutQuotes() {
        service.save("title", (Object) "My Daily Plan");

        assertEquals("My Daily Plan", current.get("title", null));
        assertEquals("My Daily Plan", service.load("title"));
        assertEquals("My Daily Plan", service.load("title", String.class));
    }

    @Test
    void storesObjectsAsJsonAndLoadsThemBack() {
        Settings settings = new Settings();
        settings.theme = "dark";
        settings.size = 14;

        service.save("settings", settings);
        Settings loaded = service.load("settings", Settings.class);

        assertTrue(current.get("settings", "").startsWith("{"));
        assertEquals("dark", loaded.theme);
        assertEquals(14, loaded.size);
    }

    @Test
    void stillLoadsPlainValuesWrittenByEarlierVersions() {
        current.put("title", "My Daily Plan");
        current.put("count", "42");
        current.put("flag", "true");
        current.put("mode", "DARK");

        assertEquals("My Daily Plan", service.load("title", String.class));
        assertEquals(42, service.load("count", int.class));
        assertEquals(true, service.load("flag", boolean.class));
        assertEquals(Mode.DARK, service.load("mode", Mode.class));
    }

    @Test
    void throwsWhenAStoredValueCannotBeConverted() {
        current.put("count", "not a number");

        assertThrows(DataStorageException.class, () -> service.load("count", int.class));
    }

    @Test
    void rejectsOversizedValuesAndSavesNothing() {
        String tooLong = "x".repeat(Preferences.MAX_VALUE_LENGTH + 1);

        assertThrows(DataStorageException.class, () -> service.save("big", (Object) tooLong));
        assertThrows(DataStorageException.class, () -> service.save("big", tooLong));

        assertNull(current.get("big", null));
    }

    @Test
    void savingNullRemovesTheKey() {
        service.save("name", "value");

        service.save("name", (String) null);

        assertNull(service.load("name"));
    }

    @Test
    void rejectsEmptyAndOverlongKeys() {
        assertThrows(IllegalArgumentException.class, () -> service.save("", "value"));
        assertThrows(DataStorageException.class,
                () -> service.save("k".repeat(Preferences.MAX_KEY_LENGTH + 1), "value"));
    }

    @Test
    void copiesLegacyValuesOnceAndDoesNotBringBackDeletedKeys() {
        legacy.put("theme", "dark");

        DefaultDataStorageService first = new DefaultDataStorageService(current, legacy);
        assertEquals("dark", first.load("theme"));

        first.save("theme", (String) null);
        DefaultDataStorageService afterRestart = new DefaultDataStorageService(current, legacy);

        assertNull(afterRestart.load("theme"));
        assertEquals("dark", legacy.get("theme", null));
    }

    @Test
    void valuesAlreadyInTheAppNodeWinOverLegacyOnes() {
        current.put("theme", "light");
        legacy.put("theme", "dark");

        DefaultDataStorageService migrated = new DefaultDataStorageService(current, legacy);

        assertEquals("light", migrated.load("theme"));
    }
}