package com.example.framework.validation;

import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import com.example.framework.model.OrderRecord;
import com.example.framework.model.ProfileRecord;
import io.restassured.response.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testng.Assert;

import java.math.BigDecimal;

/**
 * Domain validator for simple and complex nested POJO examples.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NestedPojoResponseValidator {

    public static ProfileRecord validateCreatedProfile(Response response, CreateProfileRequest request) {
        ProfileRecord profile = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 201),
                        "schemas/profile-response-schema.json"
                )
                .extract()
                .as(ProfileRecord.class);

        Assert.assertTrue(profile.getId() > 0, "Profile id should be generated.");
        Assert.assertEquals(profile.getFullName(), request.getFullName(), "Profile name should match.");
        Assert.assertEquals(profile.getEmail(), request.getEmail(), "Profile email should match.");
        Assert.assertEquals(profile.getAddress().getCity(), request.getAddress().getCity(), "Nested address city should match.");
        Assert.assertEquals(profile.getAddress().getLine1(), request.getAddress().getLine1(), "Nested address line1 should match.");
        return profile;
    }

    public static void validateFetchedProfile(Response response, ProfileRecord expected) {
        ProfileRecord profile = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/profile-response-schema.json"
                )
                .extract()
                .as(ProfileRecord.class);

        Assert.assertEquals(profile.getId(), expected.getId(), "Fetched profile id should match.");
        Assert.assertEquals(profile.getAddress().getPostalCode(), expected.getAddress().getPostalCode(), "Nested postal code should match.");
    }

    public static ProfileRecord validateUpdatedProfile(Response response, long expectedProfileId, CreateProfileRequest request) {
        ProfileRecord profile = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/profile-response-schema.json"
                )
                .extract()
                .as(ProfileRecord.class);

        Assert.assertEquals(profile.getId(), expectedProfileId, "Updated profile id should remain unchanged.");
        Assert.assertEquals(profile.getFullName(), request.getFullName(), "Updated nested profile name should match.");
        Assert.assertEquals(profile.getAddress().getCity(), request.getAddress().getCity(), "Updated nested profile city should match.");
        Assert.assertEquals(profile.getAddress().getLine1(), request.getAddress().getLine1(), "Updated nested profile line1 should match.");
        return profile;
    }

    public static ProfileRecord validatePatchedProfile(Response response, ProfileRecord expectedProfile) {
        ProfileRecord profile = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/profile-response-schema.json"
                )
                .extract()
                .as(ProfileRecord.class);

        Assert.assertEquals(profile.getId(), expectedProfile.getId(), "Patched profile id should remain unchanged.");
        Assert.assertEquals(profile.getFullName(), expectedProfile.getFullName(), "Patched profile name should reflect the merged result.");
        Assert.assertEquals(profile.getEmail(), expectedProfile.getEmail(), "Patched profile email should reflect the merged result.");
        Assert.assertEquals(profile.getAddress().getLine1(), expectedProfile.getAddress().getLine1(), "Patched profile line1 should preserve or update correctly.");
        Assert.assertEquals(profile.getAddress().getCity(), expectedProfile.getAddress().getCity(), "Patched profile city should reflect the merged result.");
        Assert.assertEquals(profile.getAddress().getPostalCode(), expectedProfile.getAddress().getPostalCode(), "Patched profile postal code should reflect the merged result.");
        Assert.assertEquals(profile.getCreatedAt(), expectedProfile.getCreatedAt(), "Patched profile should preserve the original creation timestamp.");
        return profile;
    }

    public static OrderRecord validateCreatedOrder(Response response, CreateOrderRequest request) {
        OrderRecord order = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 201),
                        "schemas/order-response-schema.json"
                )
                .extract()
                .as(OrderRecord.class);

        Assert.assertTrue(order.getId() > 0, "Order id should be generated.");
        Assert.assertEquals(order.getCustomer().getEmail(), request.getCustomer().getEmail(), "Nested customer email should match.");
        Assert.assertEquals(order.getShippingAddress().getCity(), request.getShippingAddress().getCity(), "Nested shipping city should match.");
        Assert.assertEquals(order.getPaymentDetails().getMethod(), request.getPaymentDetails().getMethod(), "Nested payment method should match.");
        Assert.assertEquals(order.getItems().size(), request.getItems().size(), "Nested item count should match.");
        Assert.assertEquals(order.getSummary().getTotalItems(), 3, "Computed total item quantity should match request quantities.");
        Assert.assertEquals(order.getSummary().getTotalAmount(), new BigDecimal("3098.00"), "Computed total amount should match nested item data.");
        Assert.assertEquals(order.getAudit().getSourceSystem(), "local-framework", "Audit metadata should be present.");
        return order;
    }

    public static void validateFetchedOrder(Response response, OrderRecord expected) {
        OrderRecord order = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/order-response-schema.json"
                )
                .extract()
                .as(OrderRecord.class);

        Assert.assertEquals(order.getId(), expected.getId(), "Fetched order id should match.");
        Assert.assertEquals(order.getCustomer().getCustomerId(), expected.getCustomer().getCustomerId(), "Nested customer id should match.");
        Assert.assertEquals(order.getItems().get(0).getSku(), expected.getItems().get(0).getSku(), "First nested item sku should match.");
        Assert.assertEquals(order.getTags(), expected.getTags(), "Nested tag list should match.");
    }

    public static OrderRecord validateUpdatedOrder(Response response, long expectedOrderId, CreateOrderRequest request) {
        OrderRecord order = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/order-response-schema.json"
                )
                .extract()
                .as(OrderRecord.class);

        Assert.assertEquals(order.getId(), expectedOrderId, "Updated order id should remain unchanged.");
        Assert.assertEquals(order.getCustomer().getEmail(), request.getCustomer().getEmail(), "Updated nested customer email should match.");
        Assert.assertEquals(order.getPaymentDetails().getMethod(), request.getPaymentDetails().getMethod(), "Updated payment method should match.");
        Assert.assertEquals(order.getItems().size(), request.getItems().size(), "Updated nested item count should match.");
        Assert.assertEquals(order.getTags(), request.getTags(), "Updated nested tags should match.");
        return order;
    }

    public static OrderRecord validatePatchedOrder(Response response, OrderRecord expectedOrder) {
        OrderRecord order = ApiSchemaValidator.validateSchema(
                        ApiResponseValidator.validateJsonResponse(response, 200),
                        "schemas/order-response-schema.json"
                )
                .extract()
                .as(OrderRecord.class);

        Assert.assertEquals(order.getId(), expectedOrder.getId(), "Patched order id should remain unchanged.");
        Assert.assertEquals(order.getCustomer().getEmail(), expectedOrder.getCustomer().getEmail(), "Patched customer email should reflect the merged result.");
        Assert.assertEquals(order.getCustomer().getCustomerId(), expectedOrder.getCustomer().getCustomerId(), "Unchanged nested customer fields should be preserved.");
        Assert.assertEquals(order.getPaymentDetails().getMethod(), expectedOrder.getPaymentDetails().getMethod(), "Patched payment method should reflect the merged result.");
        Assert.assertEquals(order.getPaymentDetails().getCurrency(), expectedOrder.getPaymentDetails().getCurrency(), "Unchanged payment fields should be preserved.");
        Assert.assertEquals(order.getItems().size(), expectedOrder.getItems().size(), "Patched items should reflect merge-patch array replacement.");
        Assert.assertEquals(order.getItems().get(0).getSku(), expectedOrder.getItems().get(0).getSku(), "Patched first item sku should match.");
        Assert.assertEquals(order.getSummary().getTotalItems(), expectedOrder.getSummary().getTotalItems(), "Patched order summary total items should be recomputed.");
        Assert.assertTrue(
                order.getSummary().getTotalAmount().compareTo(expectedOrder.getSummary().getTotalAmount()) == 0,
                "Patched order total amount should be recomputed."
        );
        Assert.assertEquals(order.getTags(), expectedOrder.getTags(), "Patched tags should reflect array replacement.");
        Assert.assertEquals(order.getAudit().getCreatedAt(), expectedOrder.getAudit().getCreatedAt(), "Patched order should preserve original audit creation time.");
        return order;
    }
}
