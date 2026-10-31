package io.github.itech_framework.core.annotations.persistences;

import io.github.itech_framework.core.annotations.ComponentScan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the application uses the ITech JPA module.
 * <p>
 * This annotation is read by the JPA module, not by the core. The core only
 * enforces one rule: if a scanned class is a JPA entity ({@code javax.persistence.Entity}
 * or {@code jakarta.persistence.Entity}) and the JPA module is missing or not
 * initialized, startup fails with a {@code FrameworkException} that explains what to add
 * or which {@code jpa.*} properties to check.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see ComponentScan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EnableJPA {
    /**
     * Enables or disables JPA configuration, as interpreted by the JPA module.
     *
     * @return {@code true} (default) to enable JPA support
     */
    boolean value() default true;

    /**
     * Package that contains the JPA entity classes, as interpreted by the JPA module.
     * <p>
     * Example: {@code @EnableJPA(entitiesPackage = "com.example.domain.entities")}
     * </p>
     *
     * @return the entity package, or an empty string to let the JPA module choose
     */
    String entitiesPackage() default "";
}