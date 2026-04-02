package com.example.framework.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central endpoint-path reader for the test framework.
 * It loads endpoint templates from one properties file so clients and tests do not hardcode paths.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EndpointConfig {
    private static final Properties ENDPOINTS = load();

    public static String health() {
        return get("api.endpoint.health");
    }

    public static String authToken() {
        return get("api.endpoint.auth.token");
    }

    public static String authRefresh() {
        return get("api.endpoint.auth.refresh");
    }

    public static String users() {
        return get("api.endpoint.users.collection");
    }

    public static String userById() {
        return get("api.endpoint.users.by-id");
    }

    public static String seedUsers() {
        return get("api.endpoint.users.seed");
    }

    public static String profiles() {
        return get("api.endpoint.profiles.collection");
    }

    public static String profileById() {
        return get("api.endpoint.profiles.by-id");
    }

    public static String orders() {
        return get("api.endpoint.orders.collection");
    }

    public static String orderById() {
        return get("api.endpoint.orders.by-id");
    }

    private static String get(String key) {
        return System.getProperty(key, ENDPOINTS.getProperty(key));
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream inputStream = EndpointConfig.class.getClassLoader().getResourceAsStream("endpoints.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("endpoints.properties not found");
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load endpoints.properties", exception);
        }
    }
}
