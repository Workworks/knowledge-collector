package com.example.knowledgecollector.web.api;

import java.util.List;

public record ApiError(String code, String message, List<FieldErrorDetail> fieldErrors) {

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }
}
