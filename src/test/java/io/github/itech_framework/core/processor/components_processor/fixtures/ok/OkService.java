package io.github.itech_framework.core.processor.components_processor.fixtures.ok;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.itech_framework.core.annotations.components.Component;
import io.github.itech_framework.core.annotations.methods.PreDestroy;

@Component
public class OkService {
    public static final AtomicInteger DESTROYED = new AtomicInteger();

    @PreDestroy
    public void stop() {
        DESTROYED.incrementAndGet();
    }
}