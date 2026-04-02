package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Stored order record returned by the API for the complex nested POJO example.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRecord {
    private long id;
    private OrderCustomer customer;
    private List<OrderItemRequest> items;
    private Address shippingAddress;
    private PaymentDetails paymentDetails;
    private List<String> tags;
    private OrderSummary summary;
    private OrderAudit audit;
}

