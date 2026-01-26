package io.github.itech_framework.core.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {
    /**
     * Creates an instance of the specified class using its no-arg constructor
     * @param clazz The class to instantiate
     * @return New instance of the class
     * @throws InstanceCreationException if instantiation fails
     */
    public static <T> T createInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException e) {
            throw new InstanceCreationException(
                    "Failed to create instance of " + clazz.getName(), e
            );
        }
    }

    /**
     * Creates an instance with constructor arguments
     * @param clazz The class to instantiate
     * @param args Constructor arguments
     * @return New instance of the class
     * @throws InstanceCreationException if instantiation fails
     */
    public static <T> T createInstance(Class<T> clazz, Object... args) {
        try {
            Class<?>[] parameterTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
            }

            Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException e) {
            throw new InstanceCreationException(
                    "Failed to create instance of " + clazz.getName(), e
            );
        }
    }

    public static class InstanceCreationException extends RuntimeException {
        /**
		 * 
		 */
		private static final long serialVersionUID = -124940439973340771L;

		public InstanceCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
