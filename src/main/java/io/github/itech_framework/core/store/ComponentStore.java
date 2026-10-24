package io.github.itech_framework.core.store;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentStore {
    private static final int DEFAULT_LEVEL = 3;
    private static final Object LOCK = new Object();

    public static final Map<String, Object> components = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Object>> instancesByLevel = new TreeMap<>();
    private static final Map<Object, Integer> instanceLevels = new IdentityHashMap<>();

    public static void registerComponent(String key, Object instance, int level) {
        components.put(key, instance);
        synchronized (LOCK) {
            if (!instanceLevels.containsKey(instance)) {
                instanceLevels.put(instance, level);
                instancesByLevel.computeIfAbsent(level, l -> new ArrayList<>()).add(instance);
            }
        }
    }

    public static <T> void registerComponent(Class<T> componentType, T instance, int level) {
        registerComponent(componentType.getName(), instance, level);
    }

    public static List<Object> getComponentsByLevel(int level) {
        synchronized (LOCK) {
            return new ArrayList<>(instancesByLevel.getOrDefault(level, List.of()));
        }
    }

    public static List<Object> getAllComponents() {
        synchronized (LOCK) {
            List<Object> all = new ArrayList<>();
            instancesByLevel.values().forEach(all::addAll);
            return all;
        }
    }

    public static int levelOf(Object instance) {
        synchronized (LOCK) {
            return instanceLevels.getOrDefault(instance, DEFAULT_LEVEL);
        }
    }

    public static <T> List<T> findByType(Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Object instance : getAllComponents()) {
            if (type.isInstance(instance)) {
                matches.add(type.cast(instance));
            }
        }
        return matches;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getComponent(Class<T> componentType) {
        return Optional.ofNullable((T) components.get(componentType.getName()));
    }

    public static boolean hasComponent(Class<?> componentType) {
        return components.containsKey(componentType.getName());
    }

    public static Object getComponent(String key) {
        return components.get(key);
    }

    public static void clear() {
        synchronized (LOCK) {
            components.clear();
            instancesByLevel.clear();
            instanceLevels.clear();
        }
    }
}