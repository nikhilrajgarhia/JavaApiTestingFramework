package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata block showing server-generated audit values for an order.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderAudit {
    private String createdAt;
    private String createdBy;
    private String sourceSystem;
}

