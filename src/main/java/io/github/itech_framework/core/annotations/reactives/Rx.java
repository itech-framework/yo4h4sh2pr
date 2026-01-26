package io.github.itech_framework.core.annotations.reactives;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Primary annotation for dependency injection of reactive components
 * <p>
 * Automatically injects reactive component instances into fields during initialization.
 * Supports both type-based and name-based injection strategies for reactive streams
 * and asynchronous components.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Type-based injection
 * @Rx
 * private UserService userService;
 * 
 * // Named component injection
 * @Rx(name = "authService")
 * private AuthService authComponent;
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @see Component
 * @deprecated Since 1.0.11, use {@link io.github.itech_framework.core.annotations.dependency.Use} instead.
 * This annotation was renamed to better reflect its purpose as a component reference rather than reactive programming.
 */
@Deprecated(since = "1.0.11", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@SuppressWarnings({"unused"})
public @interface Rx {
    /**
     * Specifies a named component for qualified injection
     * <p>
     * When multiple components of the same type exist, use this attribute to
     * specify which registered component should be injected. If not provided,
     * injection will be performed based on field type alone.
     * </p>
     * 
     * @return The unique name of the component to inject
     */
    String name() default "";
}