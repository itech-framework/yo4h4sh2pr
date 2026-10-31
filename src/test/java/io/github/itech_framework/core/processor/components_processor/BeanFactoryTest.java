package io.github.itech_framework.core.processor.components_processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.itech_framework.core.annotations.dependency.Use;
import io.github.itech_framework.core.annotations.methods.PreDestroy;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.resourcecs.CleanupRegistry;
import io.github.itech_framework.core.store.ComponentStore;

class BeanFactoryTest {

    public static class Service {
    }

    public static class Client {
        @Use
        Service service;
    }

    public static class First {
        @Use
        Second second;
    }

    public static class Second {
        @Use
        First first;
    }

    public static class CtorA {
        public CtorA(CtorB b) {
        }
    }

    public static class CtorB {
        public CtorB(CtorA a) {
        }
    }

    private ComponentStore globalBefore;
    private ComponentStore unrelatedGlobal;
    
    static final List<String> EVENTS = new ArrayList<>();

    public static class DestroyConfig {
        @PreDestroy
        void stop() {
            EVENTS.add("config");
        }
    }

    public static class DestroyDao {
        @Use
        DestroyConfig config;

        @PreDestroy
        void stop() {
            EVENTS.add("dao");
        }
    }

    @Test
    void destroysDependentsBeforeDependencies() {
        EVENTS.clear();
        ComponentStore store = new ComponentStore();
        CleanupRegistry cleanup = new CleanupRegistry();
        BeanFactory factory = new BeanFactory(store, cleanup);
        factory.define(BeanDefinition.ofClass(DestroyConfig.class.getName(), DestroyConfig.class, 3, List.of()));
        factory.define(BeanDefinition.ofClass(DestroyDao.class.getName(), DestroyDao.class, 0, List.of()));

        factory.start();
        cleanup.cleanup();

        assertEquals(List.of("dao", "config"), EVENTS);
    }

    @BeforeEach
    void swapGlobalStore() {
        globalBefore = ComponentStore.current();
        unrelatedGlobal = new ComponentStore();
        ComponentStore.setCurrent(unrelatedGlobal);
    }

    @AfterEach
    void restoreGlobalStore() {
        ComponentStore.setCurrent(globalBefore);
    }

    private static BeanDefinition definition(Class<?> type) {
        return BeanDefinition.ofClass(type.getName(), type, ComponentStore.DEFAULT_LEVEL, List.of());
    }

    @Test
    void registersAndResolvesBeansInItsOwnStoreNotTheGlobalOne() {
        ComponentStore store = new ComponentStore();
        BeanFactory factory = new BeanFactory(store, new CleanupRegistry());
        factory.define(definition(Service.class));
        factory.define(definition(Client.class));

        factory.start();

        Client client = (Client) store.getComponent(Client.class.getName());
        assertNotNull(client);
        assertSame(store.getComponent(Service.class.getName()), client.service);
        assertFalse(unrelatedGlobal.hasComponent(Service.class.getName()));
        assertFalse(unrelatedGlobal.hasComponent(Client.class.getName()));
    }

    @Test
    void reportsConstructorCyclesWithThePath() {
        ComponentStore store = new ComponentStore();
        BeanFactory factory = new BeanFactory(store, new CleanupRegistry());
        factory.define(definition(CtorA.class));
        factory.define(definition(CtorB.class));

        FrameworkException failure = assertThrows(FrameworkException.class, factory::start);

        assertTrue(failure.getMessage().contains("Circular dependency"), failure.getMessage());
        assertTrue(failure.getMessage().contains(CtorA.class.getName()), failure.getMessage());
        assertTrue(failure.getMessage().contains(CtorB.class.getName()), failure.getMessage());
    }

    @Test
    void allowsFieldCyclesBetweenSharedComponents() {
        ComponentStore store = new ComponentStore();
        BeanFactory factory = new BeanFactory(store, new CleanupRegistry());
        factory.define(definition(First.class));
        factory.define(definition(Second.class));

        factory.start();

        First first = (First) store.getComponent(First.class.getName());
        Second second = (Second) store.getComponent(Second.class.getName());
        assertSame(second, first.second);
        assertSame(first, second.first);
    }

    @Test
    void rejectsDuplicateKeys() {
        BeanFactory factory = new BeanFactory(new ComponentStore(), new CleanupRegistry());
        factory.define(definition(Service.class));

        assertThrows(FrameworkException.class, () -> factory.define(definition(Service.class)));
    }
}