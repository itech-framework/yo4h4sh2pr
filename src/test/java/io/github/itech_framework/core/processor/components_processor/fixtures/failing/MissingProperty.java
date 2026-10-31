package io.github.itech_framework.core.processor.components_processor.fixtures.failing;

import io.github.itech_framework.core.annotations.components.Component;
import io.github.itech_framework.core.annotations.properties.Property;

@Component
public class MissingProperty {
    @Property(key = "this.key.does.not.exist")
    String value;
}