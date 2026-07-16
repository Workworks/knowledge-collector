package com.example.knowledgecollector.domain.topic;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TopicRules {

    private TopicRules() {
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static List<String> normalizeTerms(String terms) {
        if (terms == null || terms.isBlank()) {
            return List.of();
        }
        return Arrays.stream(terms.split("[,，\\n]"))
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .distinct()
                .toList();
    }

    public static String serializeTerms(List<String> terms) {
        return terms == null ? "" : String.join("\n", terms);
    }
}
