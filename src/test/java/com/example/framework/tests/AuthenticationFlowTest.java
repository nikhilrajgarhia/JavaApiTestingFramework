package com.example.framework.tests;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.example.framework.base.BaseApiTest;
import com.example.framework.client.AuthApiClient;
import com.example.framework.client.AuthTokenManager;
import com.example.framework.config.EndpointConfig;
import com.example.framework.config.FrameworkConfig;
import com.example.framework.model.AuthTokenRequest;
import com.example.framework.model.AuthTokenResponse;
import com.example.framework.model.RefreshTokenRequest;
import com.example.framework.validation.ApiResponseValidator;
import com.example.framework.validation.AuthResponseValidator;

import io.restassured.RestAssured;
import static io.restassured.http.ContentType.JSON;

/**
 * Authentication-focused tests that demonstrate token issuance, unauthorized access, refresh, caching,
 * and synchronized token access under concurrent load.
 */
public class AuthenticationFlowTest extends BaseApiTest {

    /** Auth-specific tests are only meaningful when the active environment enables authentication. */
    @BeforeClass(alwaysRun = true)
    public void ensureAuthIsEnabled() {
        if (!FrameworkConfig.isAuthEnabled()) {
            throw new SkipException("Authentication tests are skipped because api.auth.enabled=false for the active environment.");
        }
    }

    /** Protected endpoints should reject requests when no bearer token is supplied. */
    @Test(groups = "smoke")
    public void shouldRejectProtectedEndpointWithoutToken() {
        AuthResponseValidator.validateUnauthorized(
                RestAssured.given().accept(JSON).when().get(EndpointConfig.users()),
                "Missing bearer token"
        );
    }

    /** A valid token should allow access to a protected endpoint. */
    @Test(groups = "smoke")
    public void shouldIssueTokenAndAllowProtectedAccess() {
        AuthTokenResponse tokenResponse = AuthResponseValidator.validateTokenResponse(
                AUTH_API_CLIENT.createToken(new AuthTokenRequest(
                        FrameworkConfig.getAuthUsername(),
                        FrameworkConfig.getAuthPassword()
                ))
        );

        ApiResponseValidator.validateJsonResponse(
                RestAssured.given()
                        .accept(JSON)
                        .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                        .when()
                        .get(EndpointConfig.users()),
                200
        );
    }

    /** Refresh tokens should rotate the token pair so long-running test runs can continue. */
    @Test(groups = "smoke")
    public void shouldRefreshTokenPair() {
        AuthTokenResponse initialTokens = AuthResponseValidator.validateTokenResponse(
                AUTH_API_CLIENT.createToken(new AuthTokenRequest(
                        FrameworkConfig.getAuthUsername(),
                        FrameworkConfig.getAuthPassword()
                ))
        );

        AuthTokenResponse refreshedTokens = AuthResponseValidator.validateTokenResponse(
                AUTH_API_CLIENT.refreshToken(new RefreshTokenRequest(initialTokens.getRefreshToken()))
        );

        Assert.assertNotEquals(
                refreshedTokens.getAccessToken(),
                initialTokens.getAccessToken(),
                "Refresh should rotate the access token."
        );
        Assert.assertNotEquals(
                refreshedTokens.getRefreshToken(),
                initialTokens.getRefreshToken(),
                "Refresh should rotate the refresh token."
        );
    }

    /** Cached tokens should be reused until invalidated instead of re-authenticating on every request. */
    @Test(groups = "smoke")
    public void shouldCacheTokenUntilInvalidated() {
        AuthTokenManager tokenManager = new AuthTokenManager(new AuthApiClient());

        String firstToken = tokenManager.getAccessToken();
        String secondToken = tokenManager.getAccessToken();

        Assert.assertEquals(firstToken, secondToken, "Token manager should reuse the cached access token.");

        tokenManager.invalidate();

        String thirdToken = tokenManager.getAccessToken();
        Assert.assertNotEquals(thirdToken, firstToken, "After invalidation the token manager should acquire a new token.");
    }

    /** Parallel token access should still resolve to one cached token thanks to synchronization. */
    @Test(groups = "smoke")
    public void shouldSynchronizeParallelTokenAccess() throws InterruptedException {
        AuthTokenManager tokenManager = new AuthTokenManager(new AuthApiClient());
        tokenManager.invalidate();

        int threadCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        Set<String> observedTokens = ConcurrentHashMap.newKeySet();

        try {
            for (int index = 0; index < threadCount; index++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        observedTokens.add(tokenManager.getAccessToken());
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while waiting for concurrent token test", exception);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

            Assert.assertTrue(completed, "Parallel token test should complete within the timeout.");
            Assert.assertEquals(observedTokens.size(), 1, "All threads should observe the same cached access token.");
        } finally {
            executorService.shutdownNow();
        }
    }
}
