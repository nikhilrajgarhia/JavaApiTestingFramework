package com.example.framework.validation;

import io.restassured.response.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testng.Assert;

/**
 * Generic validator for error responses returned by the API.
 * It centralizes status-code and message assertions for negative scenarios.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorResponseValidator {

    /**
     * Validates an error response with the expected status and checks that the message contains
     * the expected fragment for easier negative-scenario assertions.
     */
    public static void validateError(Response response, int expectedStatusCode, String expectedMessageFragment) {
        String message = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, expectedStatusCode),
                        "schemas/error-response-schema.json"
                )
                .extract()
                .path("message");

        Assert.assertTrue(
                message.contains(expectedMessageFragment),
                "Expected error message to contain: " + expectedMessageFragment + " but was: " + message
        );
    }
}
