package com.example.framework.server;

import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.UserRecord;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for user persistence so the local API can switch between in-memory and database-backed storage.
 */
public interface UserStore extends AutoCloseable {

    /** Creates and stores a new user record. */
    UserRecord create(CreateUserRequest request);

    /** Looks up a user by id. */
    Optional<UserRecord> get(long id);

    /** Replaces the stored user payload with a full update request. */
    Optional<UserRecord> update(long id, CreateUserRequest request);

    /** Applies a merge-patch style partial update to the stored user. */
    Optional<UserRecord> patch(long id, JsonNode patch);

    /** Returns all stored users. */
    List<UserRecord> getAll();

    /** Deletes a user if present. */
    boolean delete(long id);

    /** Creates many seed users for load and workflow tests. */
    List<UserRecord> seed(int count);

    /** Releases store resources when needed. */
    @Override
    default void close() {
    }
}
