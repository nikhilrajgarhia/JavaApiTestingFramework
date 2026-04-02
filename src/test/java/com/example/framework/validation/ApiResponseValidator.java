package com.example.framework.validation;

import com.example.framework.config.FrameworkConfig;
import com.example.framework.config.ResponseSpecFactory;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Shared response validation helper for API tests.
 * This utility centralizes the most common response checks so tests do not need to repeat
 * status code, response headers, JSON content-type, and response-time assertions in every method.
 * 
 * kept generic response checks in ApiResponseValidator
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiResponseValidator {

    /**
     * Validates the expected HTTP status and JSON content type, then returns a validatable response
     * for any additional chained assertions.
     */
    public static ValidatableResponse validateJsonResponse(Response response, int statusCode) {
        ValidatableResponse validatableResponse = response.then().spec(ResponseSpecFactory.jsonResponse(statusCode));
        if (FrameworkConfig.isResponseTimeAssertionEnabled()) {
            validateResponseTime(response, FrameworkConfig.getResponseTimeMaxMs());
        }
        return validatableResponse;
    }

    /**
     * Validates only response time, allowing endpoint-specific thresholds where needed.
     */
    public static void validateResponseTime(Response response, long maxResponseTimeMs) {
        long actualResponseTimeMs = response.time();
        if (actualResponseTimeMs > maxResponseTimeMs) {
            throw new AssertionError(
                    "Expected response time <= " + maxResponseTimeMs + " ms but was " + actualResponseTimeMs + " ms."
            );
        }
    }

    /**
     * Validates a JSON response and extracts the payload as a typed model object.
     */
    public static <T> T extractModel(Response response, int statusCode, Class<T> type) {
        return validateJsonResponse(response, statusCode).extract().as(type);
    }

    /**
     * Validates a JSON response and extracts the payload as a typed list.
     */
    public static <T> List<T> extractList(Response response, int statusCode, Class<T> type) {
        return validateJsonResponse(response, statusCode).extract().jsonPath().getList(".", type);
    }
}
