package com.example.framework.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Complex nested POJO request containing multiple nested objects and a nested list.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private OrderCustomer customer;
    private List<OrderItemRequest> items;
    private Address shippingAddress;
    private PaymentDetails paymentDetails;
    private List<String> tags;
}

