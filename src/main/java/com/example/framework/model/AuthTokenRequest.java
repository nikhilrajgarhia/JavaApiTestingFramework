package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload used to obtain an access token and refresh token pair.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenRequest {
    /** Username used by the client to authenticate with the token endpoint. */
    private String username;
    /** Password paired with the username for simple local authentication. */
    private String password;
}

