package io.github.itech_framework.core.resourcecs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.exceptions.FrameworkException;

public class CleanupRegistry {
    private static final Logger logger = LogManager.getLogger(CleanupRegistry.class);

    private static volatile CleanupRegistry current = new CleanupRegistry();

    private final Object lock = new Object();
    private final List<Entry> entries = new ArrayList<>();
    private long sequence;

    public static CleanupRegistry current() {
        return current;
    }

    public static void setCurrent(CleanupRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("Cleanup registry must not be null");
        }
        current = registry;
    }

    public void register(Runnable task, int priority) {
        if (task == null) {
            throw new IllegalArgumentException("Cleanup task must not be null");
        }
        synchronized (lock) {
            entries.add(new Entry(task, priority, sequence++));
        }
    }

    public int pendingCount() {
        synchronized (lock) {
            return entries.size();
        }
    }

    public void cleanup() {
        List<Entry> batch;
        synchronized (lock) {
            batch = new ArrayList<>(entries);
            entries.clear();
        }
        batch.sort(Comparator.comparingInt(Entry::priority).reversed()
                .thenComparing(Comparator.comparingLong(Entry::order).reversed()));

        logger.info("Starting resource cleanup of {} tasks", batch.size());
        FrameworkException failure = null;
        for (Entry entry : batch) {
            try {
                logger.debug("Executing cleanup task with priority {}", entry.priority());
                entry.task().run();
            } catch (Exception | LinkageError e) {
                logger.error("Cleanup task with priority {} failed", entry.priority(), e);
                if (failure == null) {
                    failure = new FrameworkException("One or more cleanup tasks failed");
                }
                failure.addSuppressed(e);
            }
        }
        logger.info("Resource cleanup completed");
        if (failure != null) {
            throw failure;
        }
    }

    private record Entry(Runnable task, int priority, long order) {}
}