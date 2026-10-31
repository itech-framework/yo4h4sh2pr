package io.github.itech_framework.core.annotations.parameters;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies a fallback value for a parameter the framework cannot resolve from a component.
 * <p>
 * Applies to constructor parameters, {@code @Component} factory method parameters and
 * {@code @OnInit} method parameters. If a registered component matches the parameter,
 * that component is used and this value is ignored; otherwise the string value is
 * converted to the parameter type. A parameter with no matching component and no
 * {@code @DefaultParameter} stops startup with a clear error.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @OnInit
 * void configure(@DefaultParameter("8080") int port,
 *                @DefaultParameter("development") String environment) { }
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultParameter {
    /**
     * The default value, written as text.
     * <p>
     * Supported target types: {@code String}, {@code char}, all primitives and their
     * wrappers, enums (exact constant name), {@code BigDecimal}, {@code BigInteger},
     * {@code Path}, {@code File}, {@code Duration} (ISO-8601 such as {@code PT5S}),
     * {@code LocalDate}, {@code LocalDateTime} and {@code LocalTime}. Any other type
     * fails with an {@code UnsupportedOperationException}.
     * </p>
     *
     * @return the default value as a String
     */
    String value();
}