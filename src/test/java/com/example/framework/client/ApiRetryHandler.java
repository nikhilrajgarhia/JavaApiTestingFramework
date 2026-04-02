package com.example.framework.client;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;

import com.example.framework.config.FrameworkConfig;

import io.restassured.response.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared retry helper for transient API failures.
 * It is intentionally conservative and should only be used for operations where retrying is safe.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiRetryHandler {

    /**
     * Executes a retryable API operation using the framework retry policy.
     * Retries are triggered for selected transport exceptions and configured transient status codes.
     */
    public static Response execute(String operationName, Callable<Response> operation) {
        int maxAttempts = Math.max(1, FrameworkConfig.getRetryMaxAttempts());
        long delayMs = Math.max(0L, FrameworkConfig.getRetryDelayMs());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Response response = operation.call();
                if (!shouldRetry(response, attempt, maxAttempts)) {
                    return response;
                }

                log.warn(
                        "Retrying API operation {} after retryable status code {} on attempt {}/{}.",
                        operationName,
                        response.statusCode(),
                        attempt,
                        maxAttempts
                );
            } catch (Exception exception) {
                if (!shouldRetry(exception, attempt, maxAttempts)) {
                    throw wrap(exception);
                }

                log.warn(
                        "Retrying API operation {} after transient exception on attempt {}/{}: {}",
                        operationName,
                        attempt,
                        maxAttempts,
                        exception.getMessage()
                );
            }

            sleep(delayMs);
        }

        throw new IllegalStateException("Retry handling exhausted without returning a response for " + operationName);
    }

    private static boolean shouldRetry(Response response, int attempt, int maxAttempts) {
        return FrameworkConfig.getRetryableStatusCodes().contains(response.statusCode()) && attempt < maxAttempts;
    }

    private static boolean shouldRetry(Exception exception, int attempt, int maxAttempts) {
        Throwable cause = exception instanceof RuntimeException && exception.getCause() != null
                ? exception.getCause()
                : exception;

        boolean retryable = cause instanceof ConnectException || cause instanceof SocketTimeoutException;
        return retryable && attempt < maxAttempts;
    }

    private static RuntimeException wrap(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Retryable API operation failed", exception);
    }

    private static void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry API operation", exception);
        }
    }
}
