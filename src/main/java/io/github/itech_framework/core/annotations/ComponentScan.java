package io.github.itech_framework.core.annotations;

import io.github.itech_framework.core.annotations.components.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Entry-point annotation that tells the framework where to look for components
 * and which property files to load.
 * <p>
 * It must be present on the class passed to
 * {@link io.github.itech_framework.core.ITechApplication#run(Class)}. If it is missing,
 * startup fails with a {@code FrameworkException}.
 * </p>
 *
 * <p>Startup order:</p>
 * <ol>
 *   <li>The listed property files are loaded.</li>
 *   <li>The base package is scanned for classes annotated with {@link Component}
 *       or a stereotype annotation built on it.</li>
 *   <li>Components are created, injected and initialized.</li>
 * </ol>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @ComponentScan(basePackage = "com.example.app", excludePackages = {"com.example.app.legacy"})
 * public class Main { }
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {
    /**
     * Base package to scan, including all sub-packages.
     * <p>
     * If the value is blank, the package of the annotated class is used. Startup fails
     * with a {@code FrameworkException} when no classes are found in the package.
     * </p>
     *
     * @return the root package name for component scanning
     */
    String basePackage();

    /**
     * Property files to load, relative to the classpath root.
     * <p>
     * Every listed file is required; a missing file stops startup. Files are loaded in
     * array order and later files override earlier ones. For each file named
     * {@code name.properties}, an optional profile file {@code name-<profile>.properties}
     * is applied on top. The active profile is resolved from, in order: the system property
     * {@code flexi.profiles.active}, the environment variable {@code FLEXI_PROFILES_ACTIVE},
     * the {@code flexi.profiles.active} key inside the loaded files, and finally the default
     * profile {@code dev}.
     * </p>
     * <p>Default: {@code {"application.properties"}}</p>
     *
     * @return array of classpath-relative property file names
     */
    String[] properties() default {"application.properties"};

    /**
     * Package names to leave out of the scan.
     * <p>
     * A class is excluded when its fully qualified name equals an entry or starts with
     * the entry followed by a dot. Blank entries are ignored.
     * </p>
     *
     * @return package or class name prefixes to exclude
     */
    String[] excludePackages() default {};

    /**
     * Controls what happens when a scanned class cannot be loaded.
     * <p>
     * When {@code true} (default) the scan fails with an exception. When {@code false}
     * the class is skipped and a warning is logged.
     * </p>
     *
     * @return {@code true} to fail on load errors, {@code false} to skip the class
     */
    boolean failOnLoadError() default true;
}