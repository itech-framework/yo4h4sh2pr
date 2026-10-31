package io.github.itech_framework.core.annotations.jfx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the application uses the ITech JavaFX module.
 * <p>
 * At startup the core checks that the JavaFX module
 * ({@code io.github.itech_framework:java-fx}) is on the classpath. If it is missing,
 * startup fails with a {@code FrameworkException} that shows the Maven dependency to
 * add. The JavaFX specific work itself (application launch, FXML loading, controller
 * wiring) is done by that module, not by the core.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EnableJavaFx {
}