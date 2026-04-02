package com.example.framework.client;

import com.example.framework.config.EndpointConfig;
import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.UserRecord;
import io.restassured.response.Response;

import java.util.List;

/**
 * Small client wrapper around Rest Assured.
 * The purpose of this class is to centralize raw HTTP calls so test methods stay focused on behavior
 * and assertions instead of request construction details.
 */
public class UserApiClient extends BaseApiClient {

    public UserApiClient() {
        this(new AuthTokenManager());
    }

    public UserApiClient(AuthTokenManager authTokenManager) {
        super(authTokenManager);
    }

    /** Calls the health endpoint to confirm the API is reachable and alive. */
    public Response health() {
        return get("health", EndpointConfig.health(), false, true);
    }

    /** Sends a create-user request using the standard JSON contract. */
    public Response createUser(CreateUserRequest request) {
        return post("createUser", EndpointConfig.users(), request, true, false);
    }

    /** Convenience helper that creates a user and immediately maps the response body to a model. */
    public UserRecord createUserAndExtract(CreateUserRequest request) {
        return createUser(request).then().extract().as(UserRecord.class);
    }

    /** Updates an existing user by id using the standard JSON contract. */
    public Response updateUser(long userId, CreateUserRequest request) {
        return put("updateUser", EndpointConfig.userById(), request, true, false, userId);
    }

    /** Partially updates an existing user by sending only the fields that should change. */
    public Response patchUser(long userId, Object request) {
        return patch("patchUser", EndpointConfig.userById(), request, true, false, userId);
    }

    /** Retrieves a single user by id. */
    public Response getUser(long userId) {
        return get("getUser", EndpointConfig.userById(), true, true, userId);
    }

    /** Convenience helper that retrieves a user and maps the body to a model. */
    public UserRecord getUserAndExtract(long userId) {
        return getUser(userId).then().extract().as(UserRecord.class);
    }

    /** Fetches all currently stored users. */
    public Response listUsers() {
        return get("listUsers", EndpointConfig.users(), true, true);
    }

    /** Convenience helper that returns the user collection directly as model objects. */
    public List<UserRecord> listUsersAndExtract() {
        return listUsers().then().extract().jsonPath().getList(".", UserRecord.class);
    }

    /** Deletes a user by id so tests can verify cleanup and lifecycle flows. */
    public Response deleteUser(long userId) {
        return delete("deleteUser", EndpointConfig.userById(), true, false, userId);
    }

    /** Calls the seed endpoint used to create many users in one request. */
    public Response seedUsers(int count) {
        return postWithoutBody("seedUsers", EndpointConfig.seedUsers(), true, false, count);
    }
}
