package io.github.itech_framework.core.resourcecs;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CleanupRegistry {
    private static final Logger logger = LogManager.getLogger(CleanupRegistry.class);
    private static final Deque<TaskWithPriority> tasks = new ConcurrentLinkedDeque<>();

    public static void register(Runnable task, int priority) {
        tasks.push(new TaskWithPriority(task, priority));
    }

    public static void addTask(Runnable task, int priority) {
        tasks.push(new TaskWithPriority(task, priority));
    }

    public static void cleanup() {
        logger.info("Starting resource cleanup...");
        try {
            TaskWithPriority next;
            while ((next = tasks.poll()) != null) {
                try {
                    logger.debug("Executing cleanup task with priority: {}", next.priority());
                    next.task().run();
                } catch (Exception e) {
                    logger.error("Cleanup task failed", e);
                }
            }
            logger.info("Resource cleanup completed.");
        } finally {
            LogManager.shutdown();
        }
    }

    private record TaskWithPriority(Runnable task, int priority) {}
}