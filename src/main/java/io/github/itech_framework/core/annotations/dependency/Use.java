package io.github.itech_framework.core.annotations.dependency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Primary annotation for dependency injection of component references
 * <p>
 * Automatically injects component instances into fields during initialization.
 * Supports both type-based and name-based injection strategies.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Type-based injection
 * @Use
 * private UserService userService;
 * 
 * // Named component injection
 * @Use(name = "authService")
 * private AuthService authComponent;
 * }</pre>
 *
 * @author Sai Zaw Myint
 * @since 1.0.11
 * @see io.github.itech_framework.core.annotations.components.Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Use {
    /**
     * Specifies a named component for qualified injection
     * <p>
     * When multiple components of the same type exist, use this attribute to
     * specify which registered component should be injected. If not provided,
     * injection will be performed based on field type alone.
     * </p>
     * 
     * @return The unique name of the component to inject
     */
    String name() default "";
    
    /**
     * Controls whether the injected component should be shared (singleton) or a new instance
     * <p>
     * When set to {@code true} (default), the same shared instance from the component store 
     * will be injected into all dependent components. This is useful for stateless services, 
     * configuration objects, or components that manage shared resources.
     * </p>
     * <p>
     * When set to {@code false}, a new instance of the component will be created for each 
     * injection point. This is useful for:
     * <ul>
     *   <li>Stateful components that should not share state between dependents</li>
     *   <li>Thread-local or request-scoped components</li>
     *   <li>Avoiding side effects between different parts of the application</li>
     *   <li>Components with mutable state that should be isolated</li>
     * </ul>
     * </p>
     * <p><strong>Important Considerations:</strong></p>
     * <ul>
     *   <li>When {@code share=false}, the new instance will also have its dependencies 
     *       injected recursively, which may lead to circular dependency issues.</li>
     *   <li>Each new instance will have its own lifecycle and will not be tracked by 
     *       the framework's cleanup registry unless explicitly configured.</li>
     *   <li>Using {@code share=false} may impact performance if the component is 
     *       expensive to create or initialize.</li>
     *   <li>Circular dependencies with {@code share=false} will be detected and 
     *       throw a {@link FrameworkException}.</li>
     * </ul>
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * // Shared service (default behavior)
     * @Use
     * private SharedService sharedService;
     * 
     * // Non-shared service - each injection gets a new instance
     * @Use(share=false)
     * private UserSession userSession;
     * 
     * // Named non-shared component
     * @Use(name="dataProcessor", share=false)
     * private DataProcessor processor;
     * }</pre>
     * 
     * @return {@code true} to use the shared singleton instance (default), 
     *         {@code false} to create a new instance for each injection
     * @see FrameworkException
     * @see ComponentStore
     */
    boolean share() default true;
}