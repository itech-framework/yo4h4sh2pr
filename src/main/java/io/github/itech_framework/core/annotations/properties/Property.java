package io.github.itech_framework.core.annotations.properties;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects values from property files into class fields
 * <p>
 * Automatically binds properties from application configuration files (.properties)
 * to class fields during component initialization. Supports type conversion and
 * default values when properties are missing.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Property(key = "server.port", defaultValue = "8080")
 * private int port;
 * 
 * @Property(key = "api.endpoint")
 * private String apiUrl;
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@SuppressWarnings({"unused"})
public @interface Property {
    /**
     * The property key to retrieve from configuration files
     * <p>
     * Supports nested properties using dot notation.
     * Example: {@code database.connection.timeout}
     * </p>
     * 
     * @return The property key to look up
     */
    String key();

    /**
     * Fallback value if the property key is not found
     * <p>
     * The default value will be converted to the field's declared type using
     * the framework's type conversion system. Leave empty to treat missing
     * properties as errors unless the field is nullable.
     * </p>
     * 
     * @return The default value as a String representation
     */
    String defaultValue() default "";
}