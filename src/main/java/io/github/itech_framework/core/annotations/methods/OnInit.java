package io.github.itech_framework.core.annotations.methods;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that runs once after the component is created and injected.
 * <p>
 * The framework calls the method after all {@code @Use}, {@code @Property} and
 * {@code @DataStorage} fields are set, so it is the place for start-up checks and
 * warm-up work. Rules:
 * </p>
 * <ul>
 *   <li>Methods run in ascending {@link #order()}. Methods with the same order run
 *       superclass first, then subclass.</li>
 *   <li>The method may have any visibility. Its parameters are resolved like
 *       {@code @Use} injection, or filled from
 *       {@link io.github.itech_framework.core.annotations.parameters.DefaultParameter};
 *       an unresolvable parameter stops startup.</li>
 *   <li>An exception thrown by the method stops startup.</li>
 *   <li>The method runs on the thread that started the application. Do not run
 *       blocking loops here: a menu or server loop inside {@code @OnInit} would stop
 *       every other component from being initialized.</li>
 * </ul>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 * @see PreDestroy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SuppressWarnings({"unused"})
public @interface OnInit {
    /**
     * Execution order inside one component.
     * <p>
     * Lower values run first; the default is 0 and negative values are allowed.
     * </p>
     * <pre>{@code
     * @OnInit(order = 0) void first() { }
     * @OnInit(order = 5) void later() { }
     * }</pre>
     *
     * @return the order value
     */
    int order() default 0;
}