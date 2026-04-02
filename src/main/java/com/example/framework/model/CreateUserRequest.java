package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload used by the API when a client wants to create a new user.
 * The server keeps this model separate from {@link UserRecord} so request data
 * stays focused on client input, while the response model can include generated
 * server-side values such as the identifier and creation time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    /** Human-readable name used to identify the user in test scenarios. */
    private String name;
    /** Email acts as a realistic unique business field for API validation. */
    private String email;
    /** Status lets tests verify different user states without extra models. */
    private String status;
}

