package com.example.framework.client;

import com.example.framework.config.FrameworkConfig;
import com.example.framework.config.RequestSpecFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Shared transport layer for all API clients in the framework.
 * It centralizes request-spec creation, retry handling, and authenticated execution behavior
 * so endpoint-specific clients can stay small and focused on domain intent.
 */
public abstract class BaseApiClient {
    /** Optional token manager used by clients that call protected endpoints. */
    private final AuthTokenManager authTokenManager;

    protected BaseApiClient() {
        this(null);
    }

    protected BaseApiClient(AuthTokenManager authTokenManager) {
        this.authTokenManager = authTokenManager;
    }

    /** Executes an API operation, applying retries only when the operation is safe to replay. */
    protected Response execute(String operationName, boolean retrySafe, Callable<Response> operation) {
        if (retrySafe && FrameworkConfig.isRetryEnabled()) {
            return ApiRetryHandler.execute(operationName, operation);
        }

        try {
            return operation.call();
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("API operation failed: " + operationName, exception);
        }
    }

    /**
     * Executes an authenticated API operation, attaching a cached bearer token and retrying once
     * on 401 after forcing a token refresh.
     */
    protected Response executeAuthenticated(String operationName, boolean retrySafe, Function<RequestSpecification, Response> operation) {
        if (authTokenManager == null) {
            throw new IllegalStateException("Authenticated API operation requested without an AuthTokenManager: " + operationName);
        }

        return execute(operationName, retrySafe, () -> {
            Response response = operation.apply(authenticatedRequest());
            if (FrameworkConfig.isAuthEnabled() && response.statusCode() == 401) {
                authTokenManager.invalidate();
                response = operation.apply(authenticatedRequest());
            }
            return response;
        });
    }

    /** Builds a JSON request specification for non-authenticated endpoints without a request body. */
    protected RequestSpecification jsonRequest() {
        return RestAssured.given().spec(RequestSpecFactory.jsonRequest());
    }

    /** Builds a JSON request specification for non-authenticated endpoints that send a body. */
    protected RequestSpecification jsonBodyRequest() {
        return RestAssured.given().spec(RequestSpecFactory.jsonBodyRequest());
    }

    /** Builds an authenticated request specification with the current bearer token when auth is enabled. */
    protected RequestSpecification authenticatedRequest() {
        return RestAssured.given().spec(RequestSpecFactory.authenticated(authTokenManager));
    }

    /** Builds an authenticated JSON-body request specification for protected body-based endpoints. */
    protected RequestSpecification authenticatedJsonBodyRequest() {
        return RestAssured.given().spec(RequestSpecFactory.authenticatedJsonBody(authTokenManager));
    }

    /** Sends a GET request using either the authenticated or non-authenticated request flow. */
    protected Response get(String operationName, String path, boolean authenticated, boolean retrySafe, Object... pathParams) {
        if (authenticated) {
            return executeAuthenticated(operationName, retrySafe, specification -> specification.when().get(path, pathParams));
        }
        return execute(operationName, retrySafe, () -> jsonRequest().when().get(path, pathParams));
    }

    /** Sends a POST request using either the authenticated or non-authenticated request flow. */
    protected Response post(String operationName, String path, Object body, boolean authenticated, boolean retrySafe, Object... pathParams) {
        if (authenticated) {
            return executeAuthenticated(operationName, retrySafe, specification ->
                    specification.contentType(ContentType.JSON).body(body).when().post(path, pathParams));
        }
        return execute(operationName, retrySafe, () -> jsonBodyRequest().body(body).when().post(path, pathParams));
    }

    /** Sends a PUT request using the authenticated request flow. */
    protected Response put(String operationName, String path, Object body, boolean authenticated, boolean retrySafe, Object... pathParams) {
        if (authenticated) {
            return executeAuthenticated(operationName, retrySafe, specification ->
                    specification.contentType(ContentType.JSON).body(body).when().put(path, pathParams));
        }
        return execute(operationName, retrySafe, () -> jsonBodyRequest().body(body).when().put(path, pathParams));
    }

    /** Sends a PATCH request using the authenticated request flow. */
    protected Response patch(String operationName, String path, Object body, boolean authenticated, boolean retrySafe, Object... pathParams) {
        if (authenticated) {
            return executeAuthenticated(operationName, retrySafe, specification ->
                    specification.contentType(ContentType.JSON).body(body).when().patch(path, pathParams));
        }
        return execute(operationName, retrySafe, () -> jsonBodyRequest().body(body).when().patch(path, pathParams));
    }

    /** Sends a POST request without a request body, useful for actions triggered only by path or query parameters. */
    protected Response postWithoutBody(String operationName, String path, boolean authenticated, boolean retrySafe, Object... pathParams) {
        if (authenticated) {
            return executeAuthenticated(operationName, retrySafe, specification -> specification.when().post(path, pathParams));
        }
        return execute(operationName, retrySafe, () -> jsonRequest().when().post(path, pathParams));
    }

    /** Sends a DELETE request using either the authenticated or non-authenticated request flow. */
    protected Response delete(String operationName, String path, boolean authenticated, boolean retrySafe, Object... pathParams) {
        if (authenticated) {
            return executeAuthenticated(operationName, retrySafe, specification -> specification.when().delete(path, pathParams));
        }
        return execute(operationName, retrySafe, () -> jsonRequest().when().delete(path, pathParams));
    }
}
