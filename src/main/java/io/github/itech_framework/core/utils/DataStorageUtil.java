package io.github.itech_framework.core.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.annotations.storage.DataStorage;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.storage.DataStorageService;
import io.github.itech_framework.core.store.ComponentStore;

public class DataStorageUtil {
    private static final Logger logger = LogManager.getLogger(DataStorageUtil.class);

    public static void save(String key, Object value) {
        checkKey(key);
        DataStorageService storage = requireStorage();

        if (value == null) {
            storage.save(key, (String) null);
            logger.debug("Removed stored value for key '{}'", key);
            return;
        }

        storage.save(key, value);
        updateComponentFields(key, value);
    }

    public static Object load(String key) {
        checkKey(key);
        return requireStorage().load(key);
    }

    public static <T> T load(String key, Class<T> type) {
        checkKey(key);
        if (type == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        return requireStorage().load(key, type);
    }

    private static void checkKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be null or blank");
        }
    }

    private static DataStorageService requireStorage() {
        Object component = ComponentStore.current().getComponent(DataStorageService.class.getName());
        if (component == null) {
            throw new FrameworkException("No DataStorageService is registered. "
                    + "Make sure ComponentProcessor.initialize() ran before using DataStorageUtil.");
        }
        return (DataStorageService) component;
    }

    private static void updateComponentFields(String key, Object value) {
    	List<Object> components = ComponentStore.current().getAllComponents();
        for (Object component : components) {
            Optional<Field> field = findField(component, key);
            if (field.isPresent()) {
                setFieldValue(component, field.get(), key, value);
            }
        }
    }

    private static Optional<Field> findField(Object component, String key) {
        for (Class<?> type = component.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                DataStorage annotation = field.getAnnotation(DataStorage.class);
                if (annotation != null
                        && (annotation.key().isEmpty() ? field.getName() : annotation.key()).equals(key)) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }

    private static void setFieldValue(Object component, Field field, String key, Object value) {
        try {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            Object converted = !fieldType.isPrimitive() && fieldType.isInstance(value)
                    ? value
                    : ObjectUtils.convertValue(value.toString(), fieldType);
            field.set(component, converted);
            logger.debug("Updated field '{}' on {} for key '{}'",
                    field.getName(), component.getClass().getName(), key);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new FrameworkException("Value for key '" + key + "' was saved, but field '" + field.getName()
                    + "' on " + component.getClass().getName() + " could not be updated", e);
        }
    }
}