package io.github.itech_framework.core.annotations.constructor;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects the constructor the framework uses to create a component.
 * <p>
 * Constructor choice follows this order:
 * </p>
 * <ol>
 *   <li>The constructor annotated with {@code @DefaultConstructor}.</li>
 *   <li>The no-argument constructor, if one exists.</li>
 *   <li>The constructor with the most parameters that can all be resolved
 *       (by a registered component or {@link io.github.itech_framework.core.annotations.parameters.DefaultParameter}).
 *       If two such constructors have the same parameter count, startup fails
 *       and one must be marked with this annotation.</li>
 * </ol>
 * <p>
 * Because the no-argument constructor wins over resolvable constructors, mark your
 * injecting constructor with this annotation when a class has both.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface DefaultConstructor {
}