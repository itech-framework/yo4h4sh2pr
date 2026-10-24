package io.github.itech_framework.core.processor.components_processor;

import java.lang.reflect.Method;
import java.util.List;

record BeanDefinition(String key, Class<?> type, int level, List<String> aliases,
                      Method factoryMethod, String factoryOwnerKey) {

    static BeanDefinition ofClass(String key, Class<?> type, int level, List<String> aliases) {
        return new BeanDefinition(key, type, level, aliases, null, null);
    }

    static BeanDefinition ofFactory(String key, Class<?> returnType, int level, List<String> aliases,
                                    Method method, String ownerKey) {
        return new BeanDefinition(key, returnType, level, aliases, method, ownerKey);
    }

    boolean isFactory() {
        return factoryMethod != null;
    }
}