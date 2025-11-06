package org.acme.exception;

import java.util.List;

public record ApiError(String message, List<String> details) {

    public static ApiError validation(List<String> details) {
        return new ApiError("Validation failed", List.copyOf(details));
    }

    public static ApiError unexpected() {
        return new ApiError("Unexpected server error", List.of());
    }
}
