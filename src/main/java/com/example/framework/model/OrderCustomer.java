package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nested customer summary used inside complex order payloads.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomer {
    private String customerId;
    private String fullName;
    private String email;
}

