package io.github.itech_framework.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.itech_framework.core.annotations.storage.DataStorage;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.storage.DataStorageService;
import io.github.itech_framework.core.store.ComponentStore;

class DataStorageUtilTest {

    public static class Base {
        @DataStorage(key = "theme")
        String theme;
    }

    public static class Child extends Base {
        @DataStorage
        int size;
    }

    static class MemoryStorage implements DataStorageService {
        final Map<String, String> values = new HashMap<>();

        @Override
        public String load(String key) {
            return values.get(key);
        }

        @Override
        public void save(String key, String value) {
            values.put(key, value);
        }

        @Override
        public <T> T load(String key, Class<T> type) {
            return type.cast(ObjectUtils.convertValue(values.get(key), type));
        }

        @Override
        public void save(String key, Object value) {
            values.put(key, String.valueOf(value));
        }
    }

    private ComponentStore previous;
    private ComponentStore store;
    private MemoryStorage storage;

    @BeforeEach
    void installStore() {
        previous = ComponentStore.current();
        store = new ComponentStore();
        storage = new MemoryStorage();
        store.registerComponent(DataStorageService.class.getName(), storage, ComponentStore.DEFAULT_LEVEL);
        ComponentStore.setCurrent(store);
    }

    @AfterEach
    void restoreStore() {
        ComponentStore.setCurrent(previous);
    }

    @Test
    void updatesAnnotatedFieldsDeclaredInASuperclass() {
        Child child = new Child();
        store.registerComponent(Child.class.getName(), child, ComponentStore.DEFAULT_LEVEL);

        DataStorageUtil.save("theme", "dark");

        assertEquals("dark", child.theme);
        assertEquals("dark", storage.values.get("theme"));
    }

    @Test
    void updatesFieldsThatUseTheFieldNameAsKey() {
        Child child = new Child();
        store.registerComponent(Child.class.getName(), child, ComponentStore.DEFAULT_LEVEL);

        DataStorageUtil.save("size", 5);

        assertEquals(5, child.size);
    }

    @Test
    void failsClearlyWhenNoStorageServiceIsRegistered() {
        ComponentStore.setCurrent(new ComponentStore());

        FrameworkException failure = assertThrows(FrameworkException.class,
                () -> DataStorageUtil.save("theme", "dark"));

        assertTrue(failure.getMessage().contains("DataStorageService"), failure.getMessage());
    }
}