package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple nested POJO request that contains a nested address object.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {
    private String fullName;
    private String email;
    private Address address;
}

