package com.example.knowledgecollector.web.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        OffsetDateTime timestamp,
        String correlationId
) {

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now(), correlationId);
    }

    public static ApiResponse<Void> failure(ApiError error, String correlationId) {
        return new ApiResponse<>(false, null, error, OffsetDateTime.now(), correlationId);
    }
}
