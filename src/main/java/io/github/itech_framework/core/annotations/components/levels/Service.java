package io.github.itech_framework.core.annotations.components.levels;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a service component in the business logic layer.
 * <p>
 * Indicates that the annotated class contains core business rules and workflow
 * implementations. This is the replacement for the deprecated {@link BusinessLogic}
 * annotation and provides the same functionality with a more conventional name.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.1.0
 * @see Component
 * @see BusinessLogic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface Service {
}