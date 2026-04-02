package com.example.framework.tests;

import com.example.framework.base.BaseApiTest;
import com.example.framework.data.NestedPojoPayloadFactory;
import com.example.framework.model.Address;
import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import com.example.framework.model.OrderAudit;
import com.example.framework.model.OrderCustomer;
import com.example.framework.model.OrderItemRequest;
import com.example.framework.model.OrderRecord;
import com.example.framework.model.OrderSummary;
import com.example.framework.model.PaymentDetails;
import com.example.framework.model.ProfileRecord;
import com.example.framework.validation.NestedPojoResponseValidator;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates both simple and complex nested POJO handling in the framework.
 */
public class NestedPojoFlowTest extends BaseApiTest {

    /** Simple nested POJO case using a profile request with one nested address object. */
    @Test(groups = "smoke")
    public void shouldCreateAndFetchSimpleNestedPojoProfile() {
        CreateProfileRequest request = NestedPojoPayloadFactory.buildProfile(1);

        ProfileRecord createdProfile = NestedPojoResponseValidator.validateCreatedProfile(
                NESTED_POJO_API_CLIENT.createProfile(request),
                request
        );
        registerProfileCleanup(createdProfile.getId());

        CreateProfileRequest updatedRequest = NestedPojoPayloadFactory.buildUpdatedProfile(1);
        ProfileRecord updatedProfile = NestedPojoResponseValidator.validateUpdatedProfile(
                NESTED_POJO_API_CLIENT.updateProfile(createdProfile.getId(), updatedRequest),
                createdProfile.getId(),
                updatedRequest
        );

        NestedPojoResponseValidator.validateFetchedProfile(
                NESTED_POJO_API_CLIENT.getProfile(updatedProfile.getId()),
                updatedProfile
        );

        Map<String, Object> patchRequest = NestedPojoPayloadFactory.buildProfilePatch(1);
        ProfileRecord patchedProfile = NestedPojoResponseValidator.validatePatchedProfile(
                NESTED_POJO_API_CLIENT.patchProfile(updatedProfile.getId(), patchRequest),
                new ProfileRecord(
                        updatedProfile.getId(),
                        updatedProfile.getFullName(),
                        patchRequest.get("email").toString(),
                        new Address(
                                updatedProfile.getAddress().getLine1(),
                                ((Map<?, ?>) patchRequest.get("address")).get("city").toString(),
                                updatedProfile.getAddress().getState(),
                                ((Map<?, ?>) patchRequest.get("address")).get("postalCode").toString(),
                                updatedProfile.getAddress().getCountry()
                        ),
                        updatedProfile.getCreatedAt()
                )
        );

        NestedPojoResponseValidator.validateFetchedProfile(
                NESTED_POJO_API_CLIENT.getProfile(patchedProfile.getId()),
                patchedProfile
        );
    }

    /** Complex nested POJO case using an order request with nested objects, nested lists, and computed server blocks. */
    @Test(groups = "smoke")
    public void shouldCreateAndFetchComplexNestedPojoOrder() {
        CreateOrderRequest request = NestedPojoPayloadFactory.buildOrder(1);

        OrderRecord createdOrder = NestedPojoResponseValidator.validateCreatedOrder(
                NESTED_POJO_API_CLIENT.createOrder(request),
                request
        );
        registerOrderCleanup(createdOrder.getId());

        CreateOrderRequest updatedRequest = NestedPojoPayloadFactory.buildUpdatedOrder(1);
        OrderRecord updatedOrder = NestedPojoResponseValidator.validateUpdatedOrder(
                NESTED_POJO_API_CLIENT.updateOrder(createdOrder.getId(), updatedRequest),
                createdOrder.getId(),
                updatedRequest
        );

        NestedPojoResponseValidator.validateFetchedOrder(
                NESTED_POJO_API_CLIENT.getOrder(updatedOrder.getId()),
                updatedOrder
        );

        Map<String, Object> patchRequest = NestedPojoPayloadFactory.buildOrderPatch(1);
        OrderRecord patchedOrder = NestedPojoResponseValidator.validatePatchedOrder(
                NESTED_POJO_API_CLIENT.patchOrder(updatedOrder.getId(), patchRequest),
                new OrderRecord(
                        updatedOrder.getId(),
                        new OrderCustomer(
                                updatedOrder.getCustomer().getCustomerId(),
                                updatedOrder.getCustomer().getFullName(),
                                ((Map<?, ?>) patchRequest.get("customer")).get("email").toString()
                        ),
                        List.of(new OrderItemRequest("SKU-1-PATCH", "Docking Station", 2, new BigDecimal("1750.00"))),
                        updatedOrder.getShippingAddress(),
                        new PaymentDetails(
                                ((Map<?, ?>) patchRequest.get("paymentDetails")).get("method").toString(),
                                updatedOrder.getPaymentDetails().getCardLastFour(),
                                updatedOrder.getPaymentDetails().getCurrency()
                        ),
                        List.of("patched", "replacement-array"),
                        new OrderSummary(2, new BigDecimal("3500.00"), updatedOrder.getPaymentDetails().getCurrency()),
                        new OrderAudit(
                                updatedOrder.getAudit().getCreatedAt(),
                                updatedOrder.getAudit().getCreatedBy(),
                                updatedOrder.getAudit().getSourceSystem()
                        )
                )
        );

        NestedPojoResponseValidator.validateFetchedOrder(
                NESTED_POJO_API_CLIENT.getOrder(patchedOrder.getId()),
                patchedOrder
        );
    }
}
