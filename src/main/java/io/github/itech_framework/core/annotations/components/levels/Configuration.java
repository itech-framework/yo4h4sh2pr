package io.github.itech_framework.core.annotations.components.levels;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.itech_framework.core.annotations.components.Component;

/**
 * Marks a class as a configuration component that produces other components.
 * <p>
 * Every method of the class that is annotated with {@link Component} and returns a
 * non-void value becomes a factory: the framework calls it once, registers the
 * returned object, and uses it for injection like any other component. Only methods
 * declared directly in the annotated class are considered (not inherited ones).
 * Method parameters are resolved the same way as constructor parameters, so they may
 * use {@code @Use} and {@code @DefaultParameter}. Returning {@code null} stops startup.
 * </p>
 * <p>
 * A configuration class has the default level 3.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Configuration
 * public class HttpConfig {
 *     @Component
 *     public HttpClient httpClient(@Use SettingsService settings) {
 *         return HttpClient.newBuilder().build();
 *     }
 * }
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.1.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface Configuration {
}