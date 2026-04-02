package com.example.framework.client;

import com.example.framework.config.EndpointConfig;
import com.example.framework.model.AuthTokenRequest;
import com.example.framework.model.AuthTokenResponse;
import com.example.framework.model.RefreshTokenRequest;
import io.restassured.response.Response;

/**
 * Low-level authentication client responsible for talking to the token and refresh endpoints.
 * It is used internally by the token manager rather than directly by normal API tests.
 */
public class AuthApiClient extends BaseApiClient {

    /** Requests a fresh token pair using the provided username/password credentials. */
    public Response createToken(AuthTokenRequest request) {
        return post("createToken", EndpointConfig.authToken(), request, false, false);
    }

    /** Requests a rotated token pair using a refresh token issued earlier by the server. */
    public Response refreshToken(RefreshTokenRequest request) {
        return post("refreshToken", EndpointConfig.authRefresh(), request, false, false);
    }

    /** Extracts a successful token-creation response as a typed model. */
    public AuthTokenResponse createTokenAndExtract(AuthTokenRequest request) {
        return createToken(request).then().statusCode(200).extract().as(AuthTokenResponse.class);
    }

    /** Extracts a successful refresh response as a typed model. */
    public AuthTokenResponse refreshTokenAndExtract(RefreshTokenRequest request) {
        return refreshToken(request).then().statusCode(200).extract().as(AuthTokenResponse.class);
    }
}
