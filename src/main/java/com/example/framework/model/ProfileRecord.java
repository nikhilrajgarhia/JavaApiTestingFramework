package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stored profile record returned by the API for the simple nested POJO example.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRecord {
    private long id;
    private String fullName;
    private String email;
    private Address address;
    private String createdAt;
}

