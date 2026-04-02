package com.example.framework.config;

import com.example.framework.client.AuthTokenManager;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Central factory for Rest Assured request specifications.
 * Reusing request specs keeps common headers and content types consistent across all API clients.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestSpecFactory {

    /** Builds a base request spec that always accepts JSON responses from the API. */
    public static RequestSpecification jsonRequest() {
        return new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .build();
    }

    /** Builds a JSON request spec for endpoints that also send a JSON request body. */
    public static RequestSpecification jsonBodyRequest() {
        return new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .build();
    }

    /** Builds an authenticated request spec, attaching a bearer token only when auth is enabled. */
    public static RequestSpecification authenticated(AuthTokenManager authTokenManager) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setAccept(ContentType.JSON);

        if (FrameworkConfig.isAuthEnabled()) {
            builder.addHeader("Authorization", "Bearer " + authTokenManager.getAccessToken());
        }

        return builder.build();
    }

    /** Builds an authenticated JSON-body request spec for protected create, put, patch, and token-like calls. */
    public static RequestSpecification authenticatedJsonBody(AuthTokenManager authTokenManager) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON);

        if (FrameworkConfig.isAuthEnabled()) {
            builder.addHeader("Authorization", "Bearer " + authTokenManager.getAccessToken());
        }

        return builder.build();
    }
}
