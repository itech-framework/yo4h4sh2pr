package io.github.itech_framework.core.annotations.dependency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Primary annotation for dependency injection of component references
 * <p>
 * Automatically injects component instances into fields during initialization.
 * Supports both type-based and name-based injection strategies.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Type-based injection
 * @Use
 * private UserService userService;
 * 
 * // Named component injection
 * @Use(name = "authService")
 * private AuthService authComponent;
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.11
 * @see io.github.itech_framework.core.annotations.components.Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Use {
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
    boolean share() default true;
}