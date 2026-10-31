package io.github.itech_framework.core.annotations.storage;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Loads a field's value from persistent storage when the component is injected.
 * <p>
 * The value is read through the registered
 * {@link io.github.itech_framework.core.storage.DataStorageService}. Unless you register
 * your own, the framework uses a default service backed by the Java Preferences API,
 * stored under the package node of the main application class (an OS-level, per-user
 * store). In the default service a key is limited to 80 characters and a value to
 * 8192 characters. To write a value back and update the matching fields of all components,
 * use {@code DataStorageUtil.save(key, value)}.
 * </p>
 * <p>
 * If nothing is stored yet, {@link #defaultValue()} is used; when that is empty the field
 * keeps the value it already had. Field types are converted from the stored JSON or text
 * (primitives, wrappers, strings, enums and Gson-serializable objects).
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @DataStorage(key = "user.theme", defaultValue = "dark")
 * private String interfaceTheme;
 *
 * @DataStorage(defaultValue = "8080")
 * private int serverPort;
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DataStorage {
    /**
     * Storage key.
     * <p>
     * If empty, the field name is used. The key is not prefixed with the class name, so
     * two fields that share a key share the same stored value.
     * </p>
     *
     * @return the storage key, or an empty string to use the field name
     */
    String key() default "";

    /**
     * Value used when nothing is stored for the key, written as text and converted to the
     * field type.
     *
     * @return the default value, or an empty string to keep the field's current value
     */
    String defaultValue() default "";
}