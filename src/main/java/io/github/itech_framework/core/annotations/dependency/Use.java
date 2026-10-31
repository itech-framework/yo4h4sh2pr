package io.github.itech_framework.core.annotations.dependency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects another component into a field or a parameter.
 * <p>
 * Works on fields, constructor parameters, {@code @Component} factory method
 * parameters and {@code @OnInit} method parameters.
 * </p>
 *
 * <p><strong>How a shared component is found</strong> (default, {@code share = true})</p>
 * <ol>
 *   <li>If {@link #name()} is given: by exact registered key, then by simple class name.
 *       A simple name shared by several components stops startup and the fully
 *       qualified name must be used.</li>
 *   <li>Otherwise by the type of the field or parameter: first by the registered
 *       class or interface name, then by the single component assignable to that
 *       type.</li>
 *   <li>If several components match the type, a {@code FrameworkException} asks you to
 *       choose one with {@link #name()}.</li>
 *   <li>If nothing matches, startup fails with a "Missing component" error.</li>
 * </ol>
 * <p>
 * Shared dependencies are initialized before the component that uses them.
 * Circular dependencies between constructors are reported with the full path.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Use
 * private UserService userService;
 *
 * @Use(name = "authService")
 * private AuthService authComponent;
 *
 * @Use(share = false)
 * private UserSession session;
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.11
 * @version 1.1.0
 * @see io.github.itech_framework.core.annotations.components.Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Use {
    /**
     * Name of the component to inject.
     * <p>
     * Use it when several components share a type or interface. Accepts a custom
     * {@code @Component(name)}, a fully qualified class name, or an unambiguous simple
     * class name (case-sensitive). When empty, injection is by type.
     * </p>
     *
     * @return the component name, or an empty string for injection by type
     */
    String name() default "";

    /**
     * Chooses between the shared instance and a private instance.
     * <p>
     * {@code true} (default): the same singleton from the component store is injected
     * everywhere.
     * </p>
     * <p>
     * {@code false}: a new instance is created for each injection point. It receives its
     * own field injection and {@code @OnInit} calls, but it is not tracked by the cleanup
     * registry, so its {@code @PreDestroy} methods are never called automatically.
     * Circular dependencies through non-shared injection throw a {@code FrameworkException}.
     * </p>
     *
     * @return {@code true} for the shared singleton, {@code false} for a new instance
     */
    boolean share() default true;
}