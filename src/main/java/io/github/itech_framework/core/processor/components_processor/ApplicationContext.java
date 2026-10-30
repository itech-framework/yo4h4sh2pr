package io.github.itech_framework.core.processor.components_processor;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.itech_framework.core.resourcecs.CleanupRegistry;
import io.github.itech_framework.core.store.ComponentStore;

public final class ApplicationContext implements AutoCloseable {
    private final ComponentStore store;
    private final CleanupRegistry cleanupRegistry;
    private final BeanFactory beanFactory;
    private final AtomicBoolean closed = new AtomicBoolean();

    ApplicationContext(ComponentStore store, CleanupRegistry cleanupRegistry, BeanFactory beanFactory) {
        if (store == null || cleanupRegistry == null || beanFactory == null) {
            throw new IllegalArgumentException("Context parts must not be null");
        }
        this.store = store;
        this.cleanupRegistry = cleanupRegistry;
        this.beanFactory = beanFactory;
    }

    public ComponentStore store() {
        return store;
    }

    public CleanupRegistry cleanupRegistry() {
        return cleanupRegistry;
    }

    BeanFactory beanFactory() {
        return beanFactory;
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cleanupRegistry.cleanup();
        }
    }
}