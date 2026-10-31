package io.github.itech_framework.core.processor.components_processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.fixtures.failing.CreatedFirst;
import io.github.itech_framework.core.processor.components_processor.fixtures.failing.FailingApp;
import io.github.itech_framework.core.processor.components_processor.fixtures.ok.OkApp;
import io.github.itech_framework.core.processor.components_processor.fixtures.ok.OkService;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.utils.PropertiesLoader;

class ComponentProcessorTest {

	@BeforeEach
	void resetState() {
	    ApplicationContext existing = ComponentProcessor.context();
	    if (existing != null) {
	        existing.close();
	    }
	    PropertiesLoader.reset();
	    OkService.DESTROYED.set(0);
	    CreatedFirst.DESTROYED.set(0);
	}

    @Test
    void failsWithAClearMessageWhenComponentScanIsMissing() {
        FrameworkException failure = assertThrows(FrameworkException.class,
                () -> ComponentProcessor.initialize(String.class));

        assertTrue(failure.getMessage().contains("@ComponentScan"), failure.getMessage());
    }

    @Test
    void registersComponentsIntoTheNewContextAndMakesItCurrent() throws Exception {
        ApplicationContext context = ComponentProcessor.initialize(OkApp.class);

        assertTrue(context.store().hasComponent(OkService.class.getName()));
        assertSame(context.store(), ComponentStore.current());
        assertSame(context, ComponentProcessor.context());
    }

    @Test
    void legacyInitializersUsingTheGlobalStoreLandInTheNewContext() throws Exception {
        ApplicationContext context = ComponentProcessor.initialize(OkApp.class);

        assertTrue(context.store().hasComponent(LegacyModuleInitializer.MARKER_KEY));
    }

    @Test
    void closingTheContextRunsPreDestroyOnce() throws Exception {
        ApplicationContext context = ComponentProcessor.initialize(OkApp.class);

        context.close();
        context.close();

        assertEquals(1, OkService.DESTROYED.get());
        assertTrue(context.isClosed());
    }

    @Test
    void initializingAgainClosesThePreviousContextAfterTheNewOneStarts() throws Exception {
        ApplicationContext first = ComponentProcessor.initialize(OkApp.class);

        ApplicationContext second = ComponentProcessor.initialize(OkApp.class);

        assertNotSame(first, second);
        assertTrue(first.isClosed());
        assertFalse(second.isClosed());
        assertSame(second, ComponentProcessor.context());
        assertSame(second.store(), ComponentStore.current());
    }

    @Test
    void aFailedStartCleansUpAndKeepsThePreviousContextIntact() throws Exception {
        ApplicationContext healthy = ComponentProcessor.initialize(OkApp.class);
        ComponentStore healthyStore = ComponentStore.current();

        assertThrows(FrameworkException.class, () -> ComponentProcessor.initialize(FailingApp.class));

        assertEquals(1, CreatedFirst.DESTROYED.get());
        assertSame(healthy, ComponentProcessor.context());
        assertSame(healthyStore, ComponentStore.current());
        assertFalse(healthy.isClosed());
        assertEquals(0, OkService.DESTROYED.get());
    }
}