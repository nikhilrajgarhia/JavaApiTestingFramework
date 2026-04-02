package com.example.framework.validation;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Shared schema-validation helper for API responses.
 * This keeps JSON schema checks reusable and prevents schema assertions from being duplicated in tests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiSchemaValidator {

    /**
     * Validates the given response body against a schema stored in the test classpath.
     */
    public static ValidatableResponse validateSchema(Response response, String classpathSchema) {
        return response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchema));
    }

    /**
     * Validates an already-validatable response against a schema stored in the test classpath.
     */
    public static ValidatableResponse validateSchema(ValidatableResponse response, String classpathSchema) {
        return response.assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchema));
    }
}
