package io.github.itech_framework.core.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.itech_framework.core.annotations.storage.DataStorage;
import io.github.itech_framework.core.storage.DataStorageService;
import io.github.itech_framework.core.store.ComponentStore;

import java.lang.reflect.Field;
import java.util.Arrays;

public class DataStorageUtil {
	private static final Logger logger = LogManager.getLogger(DataStorageUtil.class);

	public static void save(String key, Object value) {
		if (key == null || value == null)
			return;

		updateComponentField(key, value);

		DataStorageService storage = (DataStorageService) ComponentStore
				.getComponent(DataStorageService.class.getName());
		if (storage != null) {
			storage.save(key, value.toString());
		}
	}

	public static Object load(String key) {
		DataStorageService storage = (DataStorageService) ComponentStore
				.getComponent(DataStorageService.class.getName());
		if (storage != null) {
			return storage.load(key);
		}
		return null;
	}

	public static <T> T load(String key, Class<T> type) {
		DataStorageService storage = (DataStorageService) ComponentStore
				.getComponent(DataStorageService.class.getName());
		if (storage != null) {
			return storage.load(key, type);
		}
		return null;
	}

	private static void updateComponentField(String key, Object value) {
		ComponentStore.components.values().stream().filter(component -> hasMatchingField(component, key))
				.forEach(component -> setFieldValue(component, key, value));
	}

	private static boolean hasMatchingField(Object component, String key) {
		return Arrays.stream(component.getClass().getDeclaredFields()).anyMatch(f -> {
			DataStorage ann = f.getAnnotation(DataStorage.class);
			return ann != null && (ann.key().isEmpty() ? f.getName() : ann.key()).equals(key);
		});
	}

	private static void setFieldValue(Object component, String key, Object value) {
		try {
			Field field = Arrays.stream(component.getClass().getDeclaredFields()).filter(f -> {
				DataStorage ann = f.getAnnotation(DataStorage.class);
				return ann != null && (ann.key().isEmpty() ? f.getName() : ann.key()).equals(key);
			}).findFirst().orElseThrow();

			field.setAccessible(true);
			Object converted = ObjectUtils.convertValue(value.toString(), field.getType());
			field.set(component, converted);
		} catch (Exception e) {
			logger.error("Failed to update field for key: {}", key, e);
		}
	}
}
