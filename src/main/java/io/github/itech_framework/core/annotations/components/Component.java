package io.github.itech_framework.core.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.itech_framework.core.annotations.ComponentScan;

/**
 * Marks a class, or a factory method, as a managed component.
 * <p>
 * This is the base stereotype of the framework. Annotations such as
 * {@link io.github.itech_framework.core.annotations.components.levels.DataAccess},
 * {@link io.github.itech_framework.core.annotations.components.levels.Service} and
 * {@link io.github.itech_framework.core.annotations.components.levels.Presentation}
 * are meta-annotated with it, so a class carrying any of them is a component too
 * (one level of meta-annotation is recognized).
 * </p>
 *
 * <p><strong>On a class</strong></p>
 * <ul>
 *   <li>Found by {@link ComponentScan}; abstract classes and interfaces are skipped.</li>
 *   <li>Created once as a shared singleton.</li>
 *   <li>Registered under its key: the {@link #name()} value, or the fully qualified
 *       class name when no name is given.</li>
 *   <li>Also registered under each interface it implements, unless
 *       {@link IgnoreInterfaces} is present. If several components implement the same
 *       interface, that interface name becomes ambiguous and the component must be chosen
 *       by name.</li>
 * </ul>
 *
 * <p><strong>On a method</strong></p>
 * <ul>
 *   <li>Only valid inside a class annotated with
 *       {@link io.github.itech_framework.core.annotations.components.levels.Configuration}.</li>
 *   <li>The method must return a non-null object; its parameters are resolved like
 *       constructor parameters.</li>
 *   <li>The key is {@link #name()} or {@code DeclaringClass.methodName}; the return type
 *       is registered as an alias.</li>
 * </ul>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see ComponentScan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Component {
    /**
     * Custom name for the component.
     * <p>
     * Names must be unique inside one application context; a duplicate stops startup.
     * A component can later be selected with {@code @Use(name = "...")} using this name,
     * its fully qualified class name, or its simple class name when that is unambiguous
     * (the simple name is case-sensitive and keeps its original capitalization).
     * </p>
     * <p>Example: {@code @Component(name = "userService")}</p>
     *
     * @return the custom name, or an empty string to use the fully qualified class name
     */
    public String name() default "";
}