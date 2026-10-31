package io.github.itech_framework.core.annotations.reactives;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Removed annotation, kept only to produce a clear error.
 * <p>
 * {@code @Rx} used to inject component references. It was renamed to
 * {@link io.github.itech_framework.core.annotations.dependency.Use}. A field that still
 * carries {@code @Rx} is no longer injected: startup stops with a {@code FrameworkException}
 * that names the field and tells you to replace the annotation with {@code @Use}.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 * @deprecated Since 1.0.11, use {@link io.github.itech_framework.core.annotations.dependency.Use} instead.
 */
@Deprecated(since = "1.0.11", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@SuppressWarnings({"unused"})
public @interface Rx {
    /**
     * Name of the component to inject. Ignored; see the class description.
     *
     * @return the component name
     */
    String name() default "";
}