package com.example.framework.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal error payload returned by the local API when a request cannot be processed.
 * A single message field is enough for this starter framework and keeps failures easy to read.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    /** Error description returned to the client for debugging and test assertions. */
    private String message;
}

