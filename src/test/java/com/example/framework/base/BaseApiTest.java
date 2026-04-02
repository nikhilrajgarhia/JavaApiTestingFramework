package com.example.framework.base;

import com.example.framework.cleanup.TestDataCleanupManager;
import com.example.framework.client.AuthApiClient;
import com.example.framework.client.AuthTokenManager;
import com.example.framework.client.NestedPojoApiClient;
import com.example.framework.client.UserApiClient;
import com.example.framework.config.FrameworkConfig;
import com.example.framework.config.RestAssuredLoggingConfigurer;
import com.example.framework.server.LocalApiServer;
import io.restassured.RestAssured;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Base class shared by all API tests.
 * It centralizes suite lifecycle concerns such as starting the embedded server and configuring Rest Assured.
 */
public abstract class BaseApiTest {
    /** Shared auth client gives tests direct access to token and refresh endpoints when needed. */
    protected static final AuthApiClient AUTH_API_CLIENT = new AuthApiClient();
    /** Shared synchronized token cache ensures parallel tests reuse one token lifecycle. */
    protected static final AuthTokenManager AUTH_TOKEN_MANAGER = new AuthTokenManager(AUTH_API_CLIENT);
    /** Shared API client keeps test classes concise and consistent. */
    protected static final UserApiClient USER_API_CLIENT = new UserApiClient(AUTH_TOKEN_MANAGER);
    /** Shared nested-POJO client exposes simple and complex nested payload examples. */
    protected static final NestedPojoApiClient NESTED_POJO_API_CLIENT = new NestedPojoApiClient(AUTH_TOKEN_MANAGER);
    /** Holds the embedded server instance when local self-hosted execution is enabled. */
    private static LocalApiServer localApiServer;

    /** Starts the local API if needed and points Rest Assured at the configured base URL. */
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        if (FrameworkConfig.shouldStartEmbeddedServer()) {
            localApiServer = new LocalApiServer(FrameworkConfig.getServerPort());
            localApiServer.start();
        }

        RestAssured.baseURI = FrameworkConfig.getBaseUrl();
        RestAssuredLoggingConfigurer.configure();
    }

    /** Resets per-test cleanup state before each method begins on the current worker thread. */
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod() {
        TestDataCleanupManager.reset();
    }

    /** Executes registered cleanup steps after every test method so created data does not leak into later tests. */
    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        TestDataCleanupManager.cleanupCurrentTest();
    }

    /** Stops the embedded server so ports and threads are released after the suite finishes. */
    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        if (localApiServer != null) {
            localApiServer.stop();
        }
    }

    /** Registers a user for automatic deletion after the current test method completes. */
    protected void registerUserCleanup(long userId) {
        TestDataCleanupManager.register("user:" + userId, () -> USER_API_CLIENT.deleteUser(userId));
    }

    /** Registers a profile for automatic deletion after the current test method completes. */
    protected void registerProfileCleanup(long profileId) {
        TestDataCleanupManager.register("profile:" + profileId, () -> NESTED_POJO_API_CLIENT.deleteProfile(profileId));
    }

    /** Registers an order for automatic deletion after the current test method completes. */
    protected void registerOrderCleanup(long orderId) {
        TestDataCleanupManager.register("order:" + orderId, () -> NESTED_POJO_API_CLIENT.deleteOrder(orderId));
    }
}
