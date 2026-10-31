package io.github.itech_framework.core.annotations.components.policy;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a component from automatic injection and lifecycle processing.
 * <p>
 * A component carrying this annotation is still found by the scan, defined and
 * constructed, but the container does <em>not</em> inject its fields, run its
 * {@code @OnInit} methods or register its {@code @PreDestroy} methods during startup.
 * The application is expected to do that itself when the component is actually
 * needed, using the static helpers of
 * {@link io.github.itech_framework.core.processor.components_processor.ComponentProcessor}:
 * {@code injectFields}, {@code injectMethods} / {@code runInitMethods} and
 * {@code destroyInstance}.
 * </p>
 * <p>
 * Because the object is still constructed at startup, keep the constructor of a
 * disabled component light. Typical uses are classes created later by another
 * mechanism (for example UI controllers) and features that need manual setup
 * before activation.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DisableLoaded {
}