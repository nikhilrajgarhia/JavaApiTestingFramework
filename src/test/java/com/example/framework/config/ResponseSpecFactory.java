package com.example.framework.config;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Central factory for shared Rest Assured response specifications.
 * Reusing response specs keeps common status, content-type, and response-header assertions consistent.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResponseSpecFactory {

    /** Builds the default JSON response contract used by this API for a given status code. */
    public static ResponseSpecification jsonResponse(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .expectContentType(ContentType.JSON)
                .expectHeader("X-Content-Type-Options", "nosniff")
                .expectHeader("Cache-Control", "no-store")
                .build();
    }
}
