package com.example.knowledgecollector.capability.web;

import java.util.Map;

public interface WebContentProvider {
    WebResponse get(WebRequest request);

    record WebRequest(String url, String userAgent, String language, int timeoutSeconds, int maxBytes) {
    }

    record WebResponse(String finalUrl, int status, String contentType, byte[] body,
                       Map<String, String> headers) {
    }
}
