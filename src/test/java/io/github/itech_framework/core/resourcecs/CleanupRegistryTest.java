package io.github.itech_framework.core.resourcecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.core.exceptions.FrameworkException;

class CleanupRegistryTest {

	@Test
	void runsHigherPriorityFirstAndNewestFirstWithinSamePriority() {
		CleanupRegistry registry = new CleanupRegistry();
		List<String> order = new ArrayList<>();
		registry.register(() -> order.add("data"), 0);
		registry.register(() -> order.add("default-1"), 3);
		registry.register(() -> order.add("business-1"), 1);
		registry.register(() -> order.add("business-2"), 1);
		registry.register(() -> order.add("default-2"), 3);

		registry.cleanup();

		assertEquals(List.of("default-2", "default-1", "business-2", "business-1", "data"), order);
	}

	@Test
	void keepsRunningAfterFailureAndReportsAllFailures() {
		CleanupRegistry registry = new CleanupRegistry();
		List<String> order = new ArrayList<>();
		registry.register(() -> order.add("last"), 0);
		registry.register(() -> {
			throw new IllegalStateException("boom");
		}, 1);
		registry.register(() -> order.add("first"), 2);

		FrameworkException failure = assertThrows(FrameworkException.class, registry::cleanup);

		assertEquals(List.of("first", "last"), order);
		assertEquals(1, failure.getSuppressed().length);
	}

	@Test
	void cleanupRunsEachTaskOnlyOnce() {
		CleanupRegistry registry = new CleanupRegistry();
		List<String> order = new ArrayList<>();
		registry.register(() -> order.add("task"), 0);

		registry.cleanup();
		registry.cleanup();

		assertEquals(List.of("task"), order);
		assertEquals(0, registry.pendingCount());
	}
}