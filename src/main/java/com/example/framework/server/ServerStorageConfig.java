package com.example.framework.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Reads server-side storage settings from JVM properties first and then from environment variables.
 * This keeps local, Docker, and Kubernetes startup configuration consistent.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerStorageConfig {
    private static final String DEFAULT_USER_STORE_TYPE = "memory";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/api_framework";
    private static final String DEFAULT_DB_USERNAME = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";

    /** Returns which user-store implementation should be used by the local API server. */
    public static String getUserStoreType() {
        return get("server.user.store.type", "SERVER_USER_STORE_TYPE", DEFAULT_USER_STORE_TYPE);
    }

    /** Returns the JDBC URL for persistent user storage. */
    public static String getDbUrl() {
        return get("db.url", "DB_URL", DEFAULT_DB_URL);
    }

    /** Returns the JDBC username for persistent user storage. */
    public static String getDbUsername() {
        return get("db.username", "DB_USERNAME", DEFAULT_DB_USERNAME);
    }

    /** Returns the JDBC password for persistent user storage. */
    public static String getDbPassword() {
        return get("db.password", "DB_PASSWORD", DEFAULT_DB_PASSWORD);
    }

    /** Returns whether startup should create the required schema automatically when JDBC storage is used. */
    public static boolean isDbSchemaInitEnabled() {
        return Boolean.parseBoolean(get("db.schema.init.enabled", "DB_SCHEMA_INIT_ENABLED", "true"));
    }

    /** Resolves a config value from system properties first and then from environment variables. */
    private static String get(String systemPropertyKey, String environmentKey, String defaultValue) {
        String systemProperty = System.getProperty(systemPropertyKey);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return defaultValue;
    }
}
