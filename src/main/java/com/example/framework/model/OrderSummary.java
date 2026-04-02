package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Computed summary block returned for the complex order example.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummary {
    private int totalItems;
    private BigDecimal totalAmount;
    private String currency;
}

