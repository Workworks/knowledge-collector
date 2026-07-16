package com.example.knowledgecollector.capability.browser;

import java.util.Map;

public interface BrowserRenderingProvider {
    RenderedPage render(RenderPageRequest request);

    record RenderPageRequest(String url, int timeoutSeconds, Map<String, String> options) {
    }

    record RenderedPage(String finalUrl, String html, Map<String, String> metadata) {
    }
}
