package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reusable nested address model used by both simple and complex nested POJO examples.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String line1;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}

