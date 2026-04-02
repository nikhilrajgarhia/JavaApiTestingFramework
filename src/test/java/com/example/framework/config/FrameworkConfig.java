package com.example.framework.config;

import io.restassured.filter.log.LogDetail;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Central configuration reader for the test framework.
 * It merges base defaults with an environment-specific properties file and then
 * allows JVM system properties to override both at runtime.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FrameworkConfig {
    /** Base properties loaded from config.properties and the active environment file. */
    private static final Properties PROPERTIES = load();
    /** Property name used to choose the active environment. */
    private static final String ENV_KEY = "api.env";
    /** Default environment used when no explicit environment is provided. */
    private static final String DEFAULT_ENV = "local";

    /** Returns the currently active environment name. */
    public static String getEnvironment() {
        return System.getProperty(ENV_KEY, PROPERTIES.getProperty(ENV_KEY, DEFAULT_ENV));
    }

    /** Returns the base URL that Rest Assured should target for all API calls. */
    public static String getBaseUrl() {
        return get("api.base.url");
    }

    /** Returns how many request payloads the high-volume test should generate. */
    public static int getRequestCount() {
        return Integer.parseInt(get("api.request.count"));
    }

    /** Returns the configured parallel thread count for TestNG execution. */
    public static int getParallelThreads() {
        return Integer.parseInt(get("api.parallel.threads"));
    }

    /** Indicates whether the suite should boot the embedded local server automatically. */
    public static boolean shouldStartEmbeddedServer() {
        return Boolean.parseBoolean(get("api.start.embedded.server"));
    }

    /** Indicates whether every request and response should be logged. */
    public static boolean isApiLoggingEnabled() {
        return Boolean.parseBoolean(get("api.logging.enabled"));
    }

    /** Indicates whether Rest Assured should log automatically when a validation fails. */
    public static boolean isLogOnValidationFailureEnabled() {
        return Boolean.parseBoolean(get("api.logging.on.validation.failure"));
    }

    /** Indicates whether shared response-time assertions should be enforced during validation. */
    public static boolean isResponseTimeAssertionEnabled() {
        return Boolean.parseBoolean(get("api.response.time.assert.enabled"));
    }

    /** Returns the default maximum allowed response time in milliseconds for validated API calls. */
    public static long getResponseTimeMaxMs() {
        return Long.parseLong(get("api.response.time.max.ms"));
    }

    /** Returns the log detail level used by the Rest Assured request and response filters. */
    public static LogDetail getApiLoggingDetail() {
        return LogDetail.valueOf(get("api.logging.detail").toUpperCase(Locale.ROOT));
    }

    /** Indicates whether automatic retry handling should be used for eligible API calls. */
    public static boolean isRetryEnabled() {
        return Boolean.parseBoolean(get("api.retry.enabled"));
    }

    /** Returns the maximum number of attempts for retryable operations, including the first call. */
    public static int getRetryMaxAttempts() {
        return Integer.parseInt(get("api.retry.max.attempts"));
    }

    /** Returns the sleep duration between retry attempts in milliseconds. */
    public static long getRetryDelayMs() {
        return Long.parseLong(get("api.retry.delay.ms"));
    }

    /** Returns the HTTP status codes that should trigger a retry when encountered. */
    public static List<Integer> getRetryableStatusCodes() {
        return Arrays.stream(get("api.retry.retryable.status.codes").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /** Indicates whether bearer-token authentication should be attached to protected API calls. */
    public static boolean isAuthEnabled() {
        return Boolean.parseBoolean(get("api.auth.enabled"));
    }

    /** Returns the username used when the framework obtains a token from the auth endpoint. */
    public static String getAuthUsername() {
        return get("api.auth.username");
    }

    /** Returns the password used when the framework obtains a token from the auth endpoint. */
    public static String getAuthPassword() {
        return get("api.auth.password");
    }

    /** Returns the expiry skew used to refresh tokens slightly before they actually expire. */
    public static long getAuthAccessTokenExpirySkewSeconds() {
        return Long.parseLong(get("api.auth.access.token.expiry.skew.seconds"));
    }

    /** Extracts the server port from the configured base URL for embedded startup use. */
    public static int getServerPort() {
        int port = URI.create(getBaseUrl()).getPort();
        return port == -1 ? 80 : port;
    }

    /** Reads a config key, preferring JVM overrides so command-line flags win over file values. */
    private static String get(String key) {
        return System.getProperty(key, PROPERTIES.getProperty(key));
    }

    /** Loads the base config first and then overlays the active environment-specific file. */
    private static Properties load() {
        Properties properties = new Properties();
        loadInto(properties, "config.properties");

        String environment = System.getProperty(ENV_KEY, properties.getProperty(ENV_KEY, DEFAULT_ENV));
        loadInto(properties, "config-" + environment + ".properties");

        return properties;
    }

    /** Loads one classpath properties file into the provided collection. */
    private static void loadInto(Properties properties, String resourceName) {
        try (InputStream inputStream = FrameworkConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException(resourceName + " not found");
            }
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + resourceName, exception);
        }
    }
}
