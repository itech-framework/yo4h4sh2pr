package io.github.itech_framework.core.annotations.components.levels;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a service component in the business logic layer.
 * <p>
 * Specialization of {@link Component} for classes that hold business rules and
 * workflows. Service components have level 1: they are created after data access
 * components (level 0) and before presentation components (level 2). This is the
 * replacement for the deprecated {@link BusinessLogic}.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.1.0
 * @version 1.1.0
 * @see Component
 * @see BusinessLogic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface Service {
}