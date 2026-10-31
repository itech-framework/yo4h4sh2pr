package io.github.itech_framework.core.annotations.methods;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that runs when the component is shut down.
 * <p>
 * Use it to release resources, close connections or save state. The method is
 * called when:
 * </p>
 * <ul>
 *   <li>the {@code ApplicationContext} is closed, or the JVM shuts down (the
 *       framework registers a shutdown hook);</li>
 *   <li>the application calls {@code ComponentProcessor.destroyInstance(...)}
 *       explicitly.</li>
 * </ul>
 * <p>
 * Shutdown happens in reverse order of initialization, so a component is destroyed
 * before the components it depends on. The method is registered only for instances the
 * container initializes itself (shared singletons, factory beans, and objects passed to
 * {@code ComponentProcessor.injectMethods}). Instances created with
 * {@code @Use(share = false)} are not tracked. If a method throws, the remaining shutdown
 * tasks still run and a {@code FrameworkException} with the failures attached is thrown at
 * the end. There is no call on garbage collection.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 * @see OnInit
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}