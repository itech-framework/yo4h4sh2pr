package io.github.itech_framework.core.processor.components_processor;

import io.github.itech_framework.core.module.ComponentRegistry;
import io.github.itech_framework.core.module.ModuleInitializer;

public class LegacyModuleInitializer implements ModuleInitializer {
    public static final String MARKER_KEY = "test.legacy.marker";

    @Override
    public void initialize(ComponentRegistry registry) {
        new ComponentRegistry().registerComponent(MARKER_KEY, new Object(), 3);
    }
}