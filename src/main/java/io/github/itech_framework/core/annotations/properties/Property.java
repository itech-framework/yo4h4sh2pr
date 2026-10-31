package io.github.itech_framework.core.annotations.properties;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a value from the loaded property files into a field.
 * <p>
 * Values come from the files listed in
 * {@link io.github.itech_framework.core.annotations.ComponentScan#properties()}
 * (including any profile overlay) and are converted to the field type during field
 * injection. Supported field types: {@code String}, {@code char}, all primitives and
 * wrappers, enums (exact constant name), {@code BigDecimal}, {@code BigInteger},
 * {@code Path}, {@code File}, {@code Duration} (ISO-8601 such as {@code PT5S}),
 * {@code LocalDate}, {@code LocalDateTime} and {@code LocalTime}.
 * </p>
 * <p>
 * If the key is missing and {@link #defaultValue()} is empty, startup fails with a
 * {@code FrameworkException} that names the key and the field. An empty default means
 * "required", so an intentionally empty default value cannot be expressed.
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
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@SuppressWarnings({"unused"})
public @interface Property {
    /**
     * The property key to read.
     * <p>
     * Keys are matched exactly as written (for example {@code database.connection.timeout});
     * dots are part of the key and do not create nested structures.
     * </p>
     *
     * @return the property key
     */
    String key();

    /**
     * Value used when the key is not found, written as text and converted to the field type.
     * <p>
     * Leave empty to make the property required.
     * </p>
     *
     * @return the default value, or an empty string if the property is required
     */
    String defaultValue() default "";
}