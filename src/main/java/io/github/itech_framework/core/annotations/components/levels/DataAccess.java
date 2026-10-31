package io.github.itech_framework.core.annotations.components.levels;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a data access component (repository layer).
 * <p>
 * Specialization of {@link Component} for classes that read and write data:
 * repositories, DAOs and storage gateways. Data access components have level 0,
 * so they are created and initialized before service (level 1) and presentation
 * (level 2) components.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 * @see Service
 * @see Presentation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface DataAccess {
}