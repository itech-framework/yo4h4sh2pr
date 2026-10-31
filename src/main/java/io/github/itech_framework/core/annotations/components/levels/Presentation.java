package io.github.itech_framework.core.annotations.components.levels;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a presentation layer component.
 * <p>
 * Specialization of {@link Component} for classes that talk to the user or the
 * outside world: UI controllers, console menus, request handlers. Presentation
 * components have level 2, so they are created after data access (level 0) and
 * service (level 1) components.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 * @see DataAccess
 * @see Service
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface Presentation {
}