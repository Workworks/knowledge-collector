package com.example.knowledgecollector.domain.source;

import java.net.URI;
import java.util.Locale;

public final class SourceRules {

    private SourceRules() {
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            String scheme = URI.create(value.trim()).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
