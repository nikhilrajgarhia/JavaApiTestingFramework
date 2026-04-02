package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned by the token service after successful authentication or refresh.
 * It exposes both tokens and their expiry timestamps so the client framework can cache
 * and refresh them proactively.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {
    /** Short-lived access token sent on protected API requests. */
    private String accessToken;
    /** Longer-lived refresh token used to obtain a new access token pair. */
    private String refreshToken;
    /** Token type returned for standard bearer authentication headers. */
    private String tokenType;
    /** ISO-8601 timestamp after which the access token should no longer be used. */
    private String accessTokenExpiresAt;
    /** ISO-8601 timestamp after which the refresh token can no longer be exchanged. */
    private String refreshTokenExpiresAt;
}
