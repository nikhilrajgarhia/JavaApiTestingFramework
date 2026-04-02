package com.example.framework.client;

import com.example.framework.config.FrameworkConfig;
import com.example.framework.model.AuthTokenRequest;
import com.example.framework.model.AuthTokenResponse;
import com.example.framework.model.RefreshTokenRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Central token cache for authenticated test execution.
 * It keeps a cached token pair in memory, refreshes tokens before expiry, and synchronizes refresh
 * operations so parallel tests do not stampede the auth endpoints.
 */
@Slf4j
public class AuthTokenManager {
    /** Shared auth client used to obtain and refresh tokens from the API. */
    private final AuthApiClient authApiClient;
    /** Lock ensures only one thread performs token refresh/login at a time. */
    private final Object tokenLock = new Object();
    /** Cached token state reused across all API calls until it is near expiry. */
    private volatile CachedTokenState cachedTokenState;

    public AuthTokenManager() {
        this(new AuthApiClient());
    }

    public AuthTokenManager(AuthApiClient authApiClient) {
        this.authApiClient = authApiClient;
    }

    /** Returns a usable access token, refreshing or re-authenticating only when necessary. */
    public String getAccessToken() {
        CachedTokenState currentState = cachedTokenState;
        if (isAccessTokenUsable(currentState)) {
            return currentState.accessToken();
        }

        synchronized (tokenLock) {
            currentState = cachedTokenState;
            if (isAccessTokenUsable(currentState)) {
                return currentState.accessToken();
            }

            if (isRefreshTokenUsable(currentState)) {
                log.info("Refreshing access token because the cached access token is near expiry.");
                cachedTokenState = toCachedState(authApiClient.refreshTokenAndExtract(
                        new RefreshTokenRequest(currentState.refreshToken())
                ));
                return cachedTokenState.accessToken();
            }

            log.info("Creating a new token pair because no valid cached token is available.");
            cachedTokenState = toCachedState(authApiClient.createTokenAndExtract(
                    new AuthTokenRequest(FrameworkConfig.getAuthUsername(), FrameworkConfig.getAuthPassword())
            ));
            return cachedTokenState.accessToken();
        }
    }

    /** Clears the cached token pair so the next request is forced to re-authenticate. */
    public void invalidate() {
        synchronized (tokenLock) {
            cachedTokenState = null;
        }
    }

    private boolean isAccessTokenUsable(CachedTokenState state) {
        return state != null && state.accessTokenExpiresAt().isAfter(nowWithSkew());
    }

    private boolean isRefreshTokenUsable(CachedTokenState state) {
        return state != null && state.refreshTokenExpiresAt().isAfter(nowWithSkew());
    }

    private Instant nowWithSkew() {
        return Instant.now().plusSeconds(FrameworkConfig.getAuthAccessTokenExpirySkewSeconds());
    }

    private CachedTokenState toCachedState(AuthTokenResponse response) {
        return new CachedTokenState(
                response.getAccessToken(),
                response.getRefreshToken(),
                Instant.parse(response.getAccessTokenExpiresAt()),
                Instant.parse(response.getRefreshTokenExpiresAt())
        );
    }

    /**
     * Immutable cached token state so readers can reuse the same snapshot safely across threads.
     */
    private record CachedTokenState(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }
}
