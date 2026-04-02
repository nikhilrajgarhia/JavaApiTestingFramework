package com.example.framework.tests;

import com.example.framework.base.BaseApiTest;
import com.example.framework.config.EndpointConfig;
import com.example.framework.config.FrameworkConfig;
import com.example.framework.data.NestedPojoPayloadFactory;
import com.example.framework.data.UserPayloadFactory;
import com.example.framework.model.Address;
import com.example.framework.model.AuthTokenRequest;
import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.RefreshTokenRequest;
import com.example.framework.validation.ErrorResponseValidator;
import io.restassured.RestAssured;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;

/**
 * Negative-scenario coverage for validation failures, missing records, and invalid authentication flows.
 */
public class NegativeScenarioTest extends BaseApiTest {

    /** Some negative auth scenarios only apply when the active environment protects endpoints. */
    @BeforeClass(alwaysRun = true)
    public void ensureAuthIsEnabled() {
        if (!FrameworkConfig.isAuthEnabled()) {
            throw new SkipException("Negative auth tests are skipped because api.auth.enabled=false for the active environment.");
        }
    }

    /** Creating a user with missing required fields should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectCreateUserWhenRequiredFieldsAreMissing() {
        CreateUserRequest invalidRequest = new CreateUserRequest(null, "broken@example.com", "ACTIVE");

        ErrorResponseValidator.validateError(
                USER_API_CLIENT.createUser(invalidRequest),
                400,
                "name, email, and status are required"
        );
    }

    /** Endpoints should reject requests whose Accept header does not allow JSON responses. */
    @Test(groups = "negative")
    public void shouldRejectUnsupportedAcceptHeader() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .accept("text/plain")
                        .when()
                        .get(EndpointConfig.health()),
                406,
                "Accept header must allow application/json"
        );
    }

    /** JSON body endpoints should reject requests that omit the JSON content type. */
    @Test(groups = "negative")
    public void shouldRejectMissingJsonContentTypeForCreateUser() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .accept(JSON)
                        .header("Authorization", "Bearer " + AUTH_TOKEN_MANAGER.getAccessToken())
                        .body(UserPayloadFactory.build(5))
                        .when()
                        .post(EndpointConfig.users()),
                415,
                "Content-Type must be application/json"
        );
    }

    /** JSON body endpoints should reject unsupported content types even when the payload itself is valid. */
    @Test(groups = "negative")
    public void shouldRejectUnsupportedContentTypeForTokenRequest() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .contentType("text/plain")
                        .accept(JSON)
                        .body("username=bad")
                        .when()
                        .post(EndpointConfig.authToken()),
                415,
                "Content-Type must be application/json"
        );
    }

    /** Fetching a user with a non-numeric id should return HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectInvalidUserIdFormat() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .accept(JSON)
                        .header("Authorization", "Bearer " + AUTH_TOKEN_MANAGER.getAccessToken())
                        .when()
                        .get(EndpointConfig.users() + "/not-a-number"),
                400,
                "Invalid user id"
        );
    }

    /** Fetching a user that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundForMissingUser() {
        ErrorResponseValidator.validateError(
                USER_API_CLIENT.getUser(999999L),
                404,
                "User not found"
        );
    }

    /** Deleting a user that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenDeletingMissingUser() {
        ErrorResponseValidator.validateError(
                USER_API_CLIENT.deleteUser(999999L),
                404,
                "User not found"
        );
    }

    /** Updating a user that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenUpdatingMissingUser() {
        ErrorResponseValidator.validateError(
                USER_API_CLIENT.updateUser(999999L, UserPayloadFactory.buildUpdated(999999)),
                404,
                "User not found"
        );
    }

    /** Updating a user with a non-numeric id should return HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectInvalidUserIdFormatDuringUpdate() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .contentType(JSON)
                        .accept(JSON)
                        .header("Authorization", "Bearer " + AUTH_TOKEN_MANAGER.getAccessToken())
                        .body(UserPayloadFactory.buildUpdated(10))
                        .when()
                        .put(EndpointConfig.users() + "/not-a-number"),
                400,
                "Invalid user id"
        );
    }

    /** Updating a user with missing required fields should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectUpdateUserWhenRequiredFieldsAreMissing() {
        CreateUserRequest invalidRequest = new CreateUserRequest(null, "broken-update@example.com", "ACTIVE");

        ErrorResponseValidator.validateError(
                USER_API_CLIENT.updateUser(1L, invalidRequest),
                400,
                "name, email, and status are required"
        );
    }

    /** Patching a user that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenPatchingMissingUser() {
        ErrorResponseValidator.validateError(
                USER_API_CLIENT.patchUser(999999L, UserPayloadFactory.buildPatch(999999)),
                404,
                "User not found"
        );
    }

    /** Patching a user with a non-numeric id should return HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectInvalidUserIdFormatDuringPatch() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .contentType(JSON)
                        .accept(JSON)
                        .header("Authorization", "Bearer " + AUTH_TOKEN_MANAGER.getAccessToken())
                        .body(UserPayloadFactory.buildPatch(10))
                        .when()
                        .patch(EndpointConfig.users() + "/not-a-number"),
                400,
                "Invalid user id"
        );
    }

    /** Patching a user into an invalid final state should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectPatchUserWhenMergedPayloadBecomesInvalid() {
        CreateUserRequest createdUser = UserPayloadFactory.build(88);
        long userId = USER_API_CLIENT.createUserAndExtract(createdUser).getId();
        registerUserCleanup(userId);
        Map<String, Object> invalidPatch = new LinkedHashMap<>();
        invalidPatch.put("status", null);

        ErrorResponseValidator.validateError(
                USER_API_CLIENT.patchUser(userId, invalidPatch),
                400,
                "name, email, and status are required"
        );
    }

    /** Updating a profile that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenUpdatingMissingProfile() {
        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.updateProfile(999999L, NestedPojoPayloadFactory.buildUpdatedProfile(1)),
                404,
                "Profile not found"
        );
    }

    /** Updating a profile with a non-numeric id should return HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectInvalidProfileIdFormatDuringUpdate() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .contentType(JSON)
                        .accept(JSON)
                        .header("Authorization", "Bearer " + AUTH_TOKEN_MANAGER.getAccessToken())
                        .body(NestedPojoPayloadFactory.buildUpdatedProfile(2))
                        .when()
                        .put(EndpointConfig.profiles() + "/not-a-number"),
                400,
                "Invalid profile id"
        );
    }

    /** Updating a profile with an incomplete nested address should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectUpdateProfileWhenNestedAddressIsInvalid() {
        CreateProfileRequest invalidRequest = new CreateProfileRequest(
                "Broken Profile",
                "broken-profile@example.com",
                new Address(null, "Hyderabad", "Telangana", "500001", "India")
        );

        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.updateProfile(1L, invalidRequest),
                400,
                "address.line1 and address.city are required"
        );
    }

    /** Patching a profile that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenPatchingMissingProfile() {
        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.patchProfile(999999L, NestedPojoPayloadFactory.buildProfilePatch(1)),
                404,
                "Profile not found"
        );
    }

    /** Patching a profile into an invalid nested state should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectPatchProfileWhenMergedAddressBecomesInvalid() {
        long profileId = NESTED_POJO_API_CLIENT.createProfile(NestedPojoPayloadFactory.buildProfile(91))
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        registerProfileCleanup(profileId);
        Map<String, Object> invalidAddressPatch = new LinkedHashMap<>();
        invalidAddressPatch.put("line1", null);
        Map<String, Object> invalidPatch = new LinkedHashMap<>();
        invalidPatch.put("address", invalidAddressPatch);

        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.patchProfile(profileId, invalidPatch),
                400,
                "address.line1 and address.city are required"
        );
    }

    /** Updating an order that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenUpdatingMissingOrder() {
        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.updateOrder(999999L, NestedPojoPayloadFactory.buildUpdatedOrder(1)),
                404,
                "Order not found"
        );
    }

    /** Updating an order with a non-numeric id should return HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectInvalidOrderIdFormatDuringUpdate() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .contentType(JSON)
                        .accept(JSON)
                        .header("Authorization", "Bearer " + AUTH_TOKEN_MANAGER.getAccessToken())
                        .body(NestedPojoPayloadFactory.buildUpdatedOrder(3))
                        .when()
                        .put(EndpointConfig.orders() + "/not-a-number"),
                400,
                "Invalid order id"
        );
    }

    /** Updating an order without required nested blocks should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectUpdateOrderWhenRequiredBlocksAreMissing() {
        CreateOrderRequest invalidRequest = NestedPojoPayloadFactory.buildUpdatedOrder(4);
        invalidRequest.setItems(List.of());

        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.updateOrder(1L, invalidRequest),
                400,
                "customer, items, shippingAddress, and paymentDetails are required"
        );
    }

    /** Patching an order that does not exist should return HTTP 404. */
    @Test(groups = "negative")
    public void shouldReturnNotFoundWhenPatchingMissingOrder() {
        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.patchOrder(999999L, NestedPojoPayloadFactory.buildOrderPatch(1)),
                404,
                "Order not found"
        );
    }

    /** Patching an order into an invalid final state should fail validation with HTTP 400. */
    @Test(groups = "negative")
    public void shouldRejectPatchOrderWhenMergedPayloadBecomesInvalid() {
        long orderId = NESTED_POJO_API_CLIENT.createOrder(NestedPojoPayloadFactory.buildOrder(92))
                .then()
                .extract()
                .jsonPath()
                .getLong("id");
        registerOrderCleanup(orderId);

        ErrorResponseValidator.validateError(
                NESTED_POJO_API_CLIENT.patchOrder(orderId, Map.of("items", List.of())),
                400,
                "customer, items, shippingAddress, and paymentDetails are required"
        );
    }

    /** Invalid login credentials should be rejected with HTTP 401. */
    @Test(groups = "negative")
    public void shouldRejectInvalidCredentials() {
        ErrorResponseValidator.validateError(
                AUTH_API_CLIENT.createToken(new AuthTokenRequest("wrong-user", "wrong-password")),
                401,
                "Invalid username or password"
        );
    }

    /** Invalid refresh tokens should be rejected with HTTP 401. */
    @Test(groups = "negative")
    public void shouldRejectInvalidRefreshToken() {
        ErrorResponseValidator.validateError(
                AUTH_API_CLIENT.refreshToken(new RefreshTokenRequest("bad-refresh-token")),
                401,
                "Refresh token is invalid or expired"
        );
    }

    /** Protected endpoints should reject an invalid bearer token even when the header is present. */
    @Test(groups = "negative")
    public void shouldRejectInvalidBearerToken() {
        ErrorResponseValidator.validateError(
                RestAssured.given()
                        .accept(JSON)
                        .header("Authorization", "Bearer invalid-token-value")
                        .when()
                        .get(EndpointConfig.users()),
                401,
                "Access token is invalid or expired"
        );
    }
}
