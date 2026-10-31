package io.github.itech_framework.core.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stops a component from being registered under the interfaces it implements.
 * <p>
 * Normally a component is reachable by its own key and by every interface it
 * implements. With this annotation only the component's own key is registered,
 * so it must be injected by its concrete class or by name. Use it when a class
 * implements marker or library interfaces (for example {@link java.io.Serializable})
 * that should never be used for lookup, or when several components share an interface
 * and you do not want that interface name to become ambiguous.
 * </p>
 *
 * @author Sai Zaw Myint
 * @since 1.0.0
 * @version 1.1.0
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IgnoreInterfaces {
}