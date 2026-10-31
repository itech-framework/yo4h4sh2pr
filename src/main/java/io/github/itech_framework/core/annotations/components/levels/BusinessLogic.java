package io.github.itech_framework.core.annotations.components.levels;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a business logic component (level 1).
 * <p>
 * Behaves exactly like {@link Service}. It is kept only for source compatibility
 * and is scheduled for removal.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 * @deprecated Use {@link Service} instead. This annotation will be removed in a future version.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Deprecated(since = "1.1.0", forRemoval = true)
public @interface BusinessLogic {
}