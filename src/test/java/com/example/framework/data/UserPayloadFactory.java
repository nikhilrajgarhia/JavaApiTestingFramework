package com.example.framework.data;

import com.example.framework.model.CreateUserRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Factory for reusable test payloads.
 * Keeping payload generation here avoids repeating hard-coded values across many test classes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserPayloadFactory {
    /**
     * Builds a deterministic user payload for the provided index.
     * Deterministic data makes debugging easier because the same input index always produces the same values.
     */
    public static CreateUserRequest build(int index) {
        return new CreateUserRequest(
                "Automation User " + index,
                "automation-user-" + index + "@example.com",
                index % 2 == 0 ? "ACTIVE" : "INACTIVE"
        );
    }

    /**
     * Builds an updated user payload so tests can exercise PUT flows without reusing the original values.
     */
    public static CreateUserRequest buildUpdated(int index) {
        return new CreateUserRequest(
                "Updated Automation User " + index,
                "updated-automation-user-" + index + "@example.com",
                index % 2 == 0 ? "INACTIVE" : "ACTIVE"
        );
    }

    /**
     * Builds a partial patch payload that changes only a subset of user fields.
     * This demonstrates true PATCH behavior where omitted fields remain unchanged on the server.
     */
    public static Map<String, Object> buildPatch(int index) {
        return Map.of(
                "name", "Patched Automation User " + index,
                "status", index % 2 == 0 ? "PATCHED_ACTIVE" : "PATCHED_INACTIVE"
        );
    }
}
