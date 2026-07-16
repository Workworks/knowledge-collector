package com.example.knowledgecollector.capability.security;

import java.net.URI;
import java.util.List;

public interface UrlSecurityValidator {
    UrlValidationResult validate(String url);

    record UrlValidationResult(boolean valid, URI normalizedUri, List<String> resolvedAddresses,
                               String errorCode, String message) {
    }
}
