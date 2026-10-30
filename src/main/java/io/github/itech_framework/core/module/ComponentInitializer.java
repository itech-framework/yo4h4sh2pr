package io.github.itech_framework.core.module;

import io.github.itech_framework.core.resourcecs.CleanupRegistry;

public interface ComponentInitializer {
    void initializeComponent(Class<?> clazz);

    default void initializeComponent(Class<?> clazz, ComponentRegistry registry, CleanupRegistry cleanupRegistry) {
        initializeComponent(clazz);
    }
}