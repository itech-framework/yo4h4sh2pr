package io.github.itech_framework.core.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.github.itech_framework.core.exceptions.FrameworkException;

public class ComponentStore {
    public static final int DEFAULT_LEVEL = 3;

    private static volatile ComponentStore current = new ComponentStore();

    private final Object lock = new Object();
    private final Map<String, Object> components = new LinkedHashMap<>();
    private final Map<Integer, List<Object>> instancesByLevel = new TreeMap<>();
    private final Map<Object, Integer> instanceLevels = new IdentityHashMap<>();

    public static ComponentStore current() {
        return current;
    }

    public static void setCurrent(ComponentStore store) {
        if (store == null) {
            throw new IllegalArgumentException("Component store must not be null");
        }
        current = store;
    }

    public void registerComponent(String key, Object instance, int level) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Component key must not be null or blank");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Component instance must not be null for key '" + key + "'");
        }
        synchronized (lock) {
            Object existing = components.get(key);
            if (existing != null && existing != instance) {
                throw new FrameworkException("Duplicate component key: " + key);
            }
            components.put(key, instance);
            if (!instanceLevels.containsKey(instance)) {
                instanceLevels.put(instance, level);
                instancesByLevel.computeIfAbsent(level, l -> new ArrayList<>()).add(instance);
            }
        }
    }

    public <T> void registerComponent(Class<T> componentType, T instance, int level) {
        registerComponent(componentType.getName(), instance, level);
    }

    public Object getComponent(String key) {
        synchronized (lock) {
            return components.get(key);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getComponent(Class<T> componentType) {
        synchronized (lock) {
            return Optional.ofNullable((T) components.get(componentType.getName()));
        }
    }

    public boolean hasComponent(String key) {
        synchronized (lock) {
            return components.containsKey(key);
        }
    }

    public boolean hasComponent(Class<?> componentType) {
        return hasComponent(componentType.getName());
    }

    public Map<String, Object> snapshot() {
        synchronized (lock) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(components));
        }
    }

    public List<Object> getComponentsByLevel(int level) {
        synchronized (lock) {
            return new ArrayList<>(instancesByLevel.getOrDefault(level, List.of()));
        }
    }

    public List<Object> getAllComponents() {
        synchronized (lock) {
            List<Object> all = new ArrayList<>();
            instancesByLevel.values().forEach(all::addAll);
            return all;
        }
    }

    public int levelOf(Object instance) {
        synchronized (lock) {
            return instanceLevels.getOrDefault(instance, DEFAULT_LEVEL);
        }
    }

    public <T> List<T> findByType(Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Object instance : getAllComponents()) {
            if (type.isInstance(instance)) {
                matches.add(type.cast(instance));
            }
        }
        return matches;
    }

    public void clear() {
        synchronized (lock) {
            components.clear();
            instancesByLevel.clear();
            instanceLevels.clear();
        }
    }
}