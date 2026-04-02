package com.example.framework.validation;

import com.example.framework.model.AuthTokenResponse;
import io.restassured.response.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testng.Assert;

/**
 * Domain-specific validation helper for authentication responses.
 * It combines schema validation with assertions about tokens, expiry metadata, and auth failures.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthResponseValidator {

    /**
     * Validates a successful token or refresh response and returns the parsed token payload.
     */
    public static AuthTokenResponse validateTokenResponse(Response response) {
        AuthTokenResponse authTokenResponse = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/auth-token-response-schema.json"
                )
                .extract()
                .as(AuthTokenResponse.class);

        Assert.assertNotNull(authTokenResponse.getAccessToken(), "Access token should be present.");
        Assert.assertFalse(authTokenResponse.getAccessToken().isBlank(), "Access token should not be blank.");
        Assert.assertNotNull(authTokenResponse.getRefreshToken(), "Refresh token should be present.");
        Assert.assertFalse(authTokenResponse.getRefreshToken().isBlank(), "Refresh token should not be blank.");
        Assert.assertEquals(authTokenResponse.getTokenType(), "Bearer", "Token type should be Bearer.");
        Assert.assertNotNull(authTokenResponse.getAccessTokenExpiresAt(), "Access-token expiry should be provided.");
        Assert.assertNotNull(authTokenResponse.getRefreshTokenExpiresAt(), "Refresh-token expiry should be provided.");

        return authTokenResponse;
    }

    /**
     * Validates a 401 authentication failure and checks that the API explains the reason.
     */
    public static void validateUnauthorized(Response response, String expectedMessageFragment) {
        Assert.assertEquals(response.getHeader("WWW-Authenticate"), "Bearer", "Unauthorized responses should advertise Bearer authentication.");
        String message = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 401),
                        "schemas/error-response-schema.json"
                )
                .extract()
                .path("message");

        Assert.assertTrue(
                message.contains(expectedMessageFragment),
                "Expected unauthorized message to contain: " + expectedMessageFragment + " but was: " + message
        );
    }
}
