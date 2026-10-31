package io.github.itech_framework.core.processor.components_processor.fixtures.failing;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.itech_framework.core.annotations.components.levels.DataAccess;
import io.github.itech_framework.core.annotations.methods.PreDestroy;

@DataAccess
public class CreatedFirst {
    public static final AtomicInteger DESTROYED = new AtomicInteger();

    @PreDestroy
    public void stop() {
        DESTROYED.incrementAndGet();
    }
}