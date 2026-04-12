package io.github.itech_framework.core.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.itech_framework.core.annotations.ComponentScan;

/**
 * Marks a class as a managed component in the ITech Framework
 * <p>
 * Classes annotated with {@code @Component} will be automatically detected during
 * component scanning and instantiated as managed beans in the application context.
 * This is the base stereotype annotation for dependency injection.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @see ComponentScan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Component {
    /**
     * Specifies a custom name for the component
     * <p>
     * If not provided, the framework will generate a default name using
     * the class name with the first letter in lowercase. Component names
     * must be unique within the application context.
     * </p>
     * <p>
     * Example: {@code @Component(name = "userService")}
     * </p>
     * 
     * @return The custom component name, or empty string for default naming
     */
    public String name() default "";
}