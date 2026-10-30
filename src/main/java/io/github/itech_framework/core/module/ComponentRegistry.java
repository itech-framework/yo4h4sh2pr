package io.github.itech_framework.core.module;

import io.github.itech_framework.core.store.ComponentStore;

public class ComponentRegistry {
    private final ComponentStore store;

    public ComponentRegistry() {
        this.store = null;
    }

    public ComponentRegistry(ComponentStore store) {
        if (store == null) {
            throw new IllegalArgumentException("Component store must not be null");
        }
        this.store = store;
    }

    private ComponentStore target() {
        return store != null ? store : ComponentStore.current();
    }

    public void registerComponent(String name, Object component, int level) {
        target().registerComponent(name, component, level);
    }

    public <T> void registerComponent(Class<T> componentType, T componentInstance, int level) {
        validateComponentType(componentType, componentInstance);
        target().registerComponent(componentType.getName(), componentInstance, level);
    }

    public void registerComponent(Object componentInstance, int level) {
        Class<?> componentType = componentInstance.getClass();
        target().registerComponent(componentType.getName(), componentInstance, level);
    }

    public <T> void registerComponentAsInterface(Class<T> interfaceType, T implementation, int level) {
        validateInterface(interfaceType);
        target().registerComponent(interfaceType.getName(), implementation, level);
    }
    
    public boolean hasComponent(String name) {
        return target().hasComponent(name);
    }

    private <T> void validateComponentType(Class<T> expectedType, Object component) {
        if (!expectedType.isInstance(component)) {
            throw new IllegalArgumentException(
                    String.format("Component type mismatch. Expected %s but got %s",
                            expectedType.getName(),
                            component.getClass().getName())
            );
        }
    }

    private <T> void validateInterface(Class<T> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException(
                    String.format("Registration as interface failed: %s is not an interface",
                            type.getName())
            );
        }
    }
}