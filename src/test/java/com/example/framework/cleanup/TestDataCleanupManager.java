package com.example.framework.cleanup;

import io.restassured.response.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Central per-test cleanup registry.
 * Tests register delete actions for records they create, and teardown executes them in reverse order.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestDataCleanupManager {
    /** Thread-local storage keeps cleanup actions isolated even when TestNG runs methods in parallel. */
    private static final ThreadLocal<Deque<CleanupTask>> CLEANUP_TASKS = ThreadLocal.withInitial(ArrayDeque::new);

    /** Clears any stale cleanup actions before a new test method starts on the same worker thread. */
    public static void reset() {
        CLEANUP_TASKS.get().clear();
    }

    /** Registers a cleanup action that should run after the current test method completes. */
    public static void register(String description, CleanupOperation operation) {
        CLEANUP_TASKS.get().push(new CleanupTask(description, operation));
    }

    /** Executes all registered cleanup actions in reverse order and then clears the current thread state. */
    public static void cleanupCurrentTest() {
        Deque<CleanupTask> cleanupTasks = CLEANUP_TASKS.get();

        while (!cleanupTasks.isEmpty()) {
            CleanupTask cleanupTask = cleanupTasks.pop();
            try {
                Response response = cleanupTask.operation().execute();
                if (response == null) {
                    log.warn("Cleanup action '{}' returned no response", cleanupTask.description());
                    continue;
                }

                int statusCode = response.statusCode();
                if (statusCode != 200 && statusCode != 204 && statusCode != 404) {
                    log.warn("Cleanup action '{}' returned unexpected status {}", cleanupTask.description(), statusCode);
                }
            } catch (Exception exception) {
                log.warn("Cleanup action '{}' failed: {}", cleanupTask.description(), exception.toString());
            }
        }

        CLEANUP_TASKS.remove();
    }

    /** Functional contract for a single cleanup operation, usually a DELETE call through an API client. */
    @FunctionalInterface
    public interface CleanupOperation {
        Response execute();
    }

    /** Small record keeps the cleanup description alongside the executable action for clearer logs. */
    private record CleanupTask(String description, CleanupOperation operation) {
    }
}
