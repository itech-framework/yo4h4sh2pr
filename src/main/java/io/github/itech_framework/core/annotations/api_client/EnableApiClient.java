package io.github.itech_framework.core.annotations.api_client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Activates API client support on the main application class.
 * <p>
 * When this annotation is present, the core loads every {@code ComponentInitializer}
 * service found through {@link java.util.ServiceLoader} and calls it once for each
 * scanned class. The API client module uses this hook to find its client interfaces
 * and register generated implementations. Use it together with
 * {@link io.github.itech_framework.core.annotations.ComponentScan}.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableApiClient {
    /**
     * Reserved switch for API client auto-configuration.
     * <p>
     * In this version the core only checks whether the annotation is present; the
     * value is not evaluated. Remove the annotation to turn API client support off.
     * </p>
     *
     * @return {@code true} by default
     */
    boolean value() default true;

}