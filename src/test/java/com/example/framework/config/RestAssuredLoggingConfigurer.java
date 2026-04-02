package com.example.framework.config;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies Rest Assured logging behavior from framework configuration.
 * Centralizing filter registration keeps logging setup out of individual test classes and clients.
 */
public final class RestAssuredLoggingConfigurer {
    private RestAssuredLoggingConfigurer() {
    }

    /**
     * Configures global Rest Assured logging based on environment properties.
     * Validation-failure logging is enabled separately from full request/response logging so
     * regular runs can stay quiet while failures still surface useful payload details.
     */
    public static void configure() {
        RestAssured.replaceFiltersWith(buildFilters());

        if (FrameworkConfig.isLogOnValidationFailureEnabled()) {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        }
    }

    private static List<Filter> buildFilters() {
        List<Filter> filters = new ArrayList<>();
        if (FrameworkConfig.isApiLoggingEnabled()) {
            filters.add(new RequestLoggingFilter(FrameworkConfig.getApiLoggingDetail()));
            filters.add(new ResponseLoggingFilter(FrameworkConfig.getApiLoggingDetail()));
        }
        return filters;
    }
}

