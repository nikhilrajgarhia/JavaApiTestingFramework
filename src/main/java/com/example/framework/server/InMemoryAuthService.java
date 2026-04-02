package com.example.framework.server;

import com.example.framework.model.AuthTokenResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory authentication service used by the local API server.
 * It issues bearer tokens, validates access tokens, rotates refresh tokens, and removes
 * expired token pairs without requiring an external identity provider.
 */
public class InMemoryAuthService {
    private static final String DEFAULT_USERNAME = "demo-api-user";
    private static final String DEFAULT_PASSWORD = "demo-api-password";
    private static final long DEFAULT_ACCESS_TOKEN_TTL_SECONDS = 20L;
    private static final long DEFAULT_REFRESH_TOKEN_TTL_SECONDS = 120L;

    /** Access-token lookup used when protected endpoints validate incoming bearer tokens. */
    private final ConcurrentMap<String, TokenSession> accessTokens = new ConcurrentHashMap<>();
    /** Refresh-token lookup used when the client asks for a new token pair. */
    private final ConcurrentMap<String, TokenSession> refreshTokens = new ConcurrentHashMap<>();
    /** Demo username accepted by the embedded token endpoint. */
    private final String username;
    /** Demo password accepted by the embedded token endpoint. */
    private final String password;
    /** Access-token lifetime keeps tokens short enough to exercise refresh handling in tests. */
    private final long accessTokenTtlSeconds;
    /** Refresh-token lifetime lets long-running suites recover access without logging in again immediately. */
    private final long refreshTokenTtlSeconds;

    public InMemoryAuthService() {
        this(
                System.getProperty("server.auth.username", DEFAULT_USERNAME),
                System.getProperty("server.auth.password", DEFAULT_PASSWORD),
                Long.parseLong(System.getProperty("server.auth.access.ttl.seconds", String.valueOf(DEFAULT_ACCESS_TOKEN_TTL_SECONDS))),
                Long.parseLong(System.getProperty("server.auth.refresh.ttl.seconds", String.valueOf(DEFAULT_REFRESH_TOKEN_TTL_SECONDS)))
        );
    }

    public InMemoryAuthService(String username, String password, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {
        this.username = username;
        this.password = password;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    /**
     * Authenticates a username/password pair and returns a fresh bearer-token response when valid.
     */
    public Optional<AuthTokenResponse> authenticate(String incomingUsername, String incomingPassword) {
        if (!username.equals(incomingUsername) || !password.equals(incomingPassword)) {
            return Optional.empty();
        }

        return Optional.of(createSession());
    }

    /**
     * Rotates an existing refresh token into a new token pair when the refresh token is still valid.
     */
    public Optional<AuthTokenResponse> refresh(String refreshToken) {
        TokenSession existingSession = refreshTokens.get(refreshToken);
        if (existingSession == null) {
            return Optional.empty();
        }

        if (existingSession.refreshTokenExpiresAt().isBefore(Instant.now())) {
            removeSession(existingSession);
            return Optional.empty();
        }

        removeSession(existingSession);
        return Optional.of(createSession());
    }

    /**
     * Returns whether an access token is present and still within its lifetime window.
     */
    public boolean isAccessTokenValid(String accessToken) {
        TokenSession session = accessTokens.get(accessToken);
        if (session == null) {
            return false;
        }

        if (session.accessTokenExpiresAt().isBefore(Instant.now())) {
            removeSession(session);
            return false;
        }

        return true;
    }

    private AuthTokenResponse createSession() {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(accessTokenTtlSeconds);
        Instant refreshExpiresAt = now.plusSeconds(refreshTokenTtlSeconds);

        TokenSession session = new TokenSession(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                accessExpiresAt,
                refreshExpiresAt
        );

        accessTokens.put(session.accessToken(), session);
        refreshTokens.put(session.refreshToken(), session);

        return new AuthTokenResponse(
                session.accessToken(),
                session.refreshToken(),
                "Bearer",
                session.accessTokenExpiresAt().toString(),
                session.refreshTokenExpiresAt().toString()
        );
    }

    private void removeSession(TokenSession session) {
        accessTokens.remove(session.accessToken());
        refreshTokens.remove(session.refreshToken());
    }

    /**
     * Stored token pair plus expiry times for both access and refresh paths.
     */
    private record TokenSession(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }
}
