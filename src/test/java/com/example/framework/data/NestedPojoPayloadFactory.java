package com.example.framework.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.framework.model.Address;
import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import com.example.framework.model.OrderCustomer;
import com.example.framework.model.OrderItemRequest;
import com.example.framework.model.PaymentDetails;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Factory for nested POJO payload examples used by the framework.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NestedPojoPayloadFactory {

    public static CreateProfileRequest buildProfile(int index) {
        return new CreateProfileRequest(
                "Nested User " + index,
                "nested-user-" + index + "@example.com",
                new Address(
                        "Street " + index,
                        "Bengaluru",
                        "Karnataka",
                        "5600" + index % 10,
                        "India"
                )
        );
    }

    public static CreateOrderRequest buildOrder(int index) {
        return new CreateOrderRequest(
                new OrderCustomer(
                        "customer-" + index,
                        "Order Customer " + index,
                        "order-customer-" + index + "@example.com"
                ),
                List.of(
                        new OrderItemRequest("SKU-" + index + "-A", "Laptop Bag", 1, new BigDecimal("1499.50")),
                        new OrderItemRequest("SKU-" + index + "-B", "Wireless Mouse", 2, new BigDecimal("799.25"))
                ),
                new Address(
                        "Shipping Lane " + index,
                        "Hyderabad",
                        "Telangana",
                        "5000" + index % 10,
                        "India"
                ),
                new PaymentDetails("CARD", "4242", "INR"),
                List.of("nested", "complex", "priority")
        );
    }

    public static CreateProfileRequest buildUpdatedProfile(int index) {
        return new CreateProfileRequest(
                "Updated Nested User " + index,
                "updated-nested-user-" + index + "@example.com",
                new Address(
                        "Updated Street " + index,
                        "Chennai",
                        "Tamil Nadu",
                        "6000" + index % 10,
                        "India"
                )
        );
    }

    public static CreateOrderRequest buildUpdatedOrder(int index) {
        return new CreateOrderRequest(
                new OrderCustomer(
                        "updated-customer-" + index,
                        "Updated Order Customer " + index,
                        "updated-order-customer-" + index + "@example.com"
                ),
                List.of(
                        new OrderItemRequest("SKU-" + index + "-C", "Mechanical Keyboard", 1, new BigDecimal("2499.00")),
                        new OrderItemRequest("SKU-" + index + "-D", "USB Hub", 3, new BigDecimal("350.00"))
                ),
                new Address(
                        "Updated Shipping Lane " + index,
                        "Pune",
                        "Maharashtra",
                        "4110" + index % 10,
                        "India"
                ),
                new PaymentDetails("UPI", "0000", "INR"),
                List.of("updated", "complex", "express")
        );
    }

    /**
     * Builds a partial profile patch that updates selected top-level and nested address fields only.
     */
    public static Map<String, Object> buildProfilePatch(int index) {
        return Map.of(
                "email", "patched-nested-user-" + index + "@example.com",
                "address", Map.of(
                        "city", "Mumbai",
                        "postalCode", "4000" + index % 10
                )
        );
    }

    /**
     * Builds a partial order patch that demonstrates deep merge for objects and full replacement for arrays.
     */
    public static Map<String, Object> buildOrderPatch(int index) {
        return Map.of(
                "customer", Map.of(
                        "email", "patched-order-customer-" + index + "@example.com"
                ),
                "paymentDetails", Map.of(
                        "method", "NETBANKING"
                ),
                "items", List.of(
                        Map.of(
                                "sku", "SKU-" + index + "-PATCH",
                                "productName", "Docking Station",
                                "quantity", 2,
                                "unitPrice", new BigDecimal("1750.00")
                        )
                ),
                "tags", List.of("patched", "replacement-array")
        );
    }
}
