package io.github.itech_framework.core.annotations.methods;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for immediate execution after component registration
 * <p>
 * Annotated methods will be automatically invoked during the initialization phase
 * following successful component registration and dependency injection. Execution
 * order can be controlled using the {@code order} parameter.
 * </p>
 * 
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Post-construction initialization logic</li>
 *   <li>Resource warm-up/preloading</li>
 *   <li>Dependency validation checks</li>
 * </ul>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SuppressWarnings({"unused"})
public @interface OnInit {
    /**
     * Determines method execution order within the component lifecycle
     * <p>
     * Methods with lower order values execute first. Default execution order is 0.
     * Negative values are allowed for high-priority initialization.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * @OnInit(order = 0) // Executes first
     * void highPriorityInit() {...}
     * 
     * @OnInit(order = 5) // Executes later
     * void lowPriorityInit() {...}
     * }</pre>
     * </p>
     * @return The initialization priority value
     */
    int order() default 0;
}