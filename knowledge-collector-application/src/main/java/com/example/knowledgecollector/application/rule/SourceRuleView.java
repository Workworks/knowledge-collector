package com.example.knowledgecollector.application.rule;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record SourceRuleView(
        Long id, Long sourceId, int version, String listSelector, String linkSelector,
        String titleSelector, String contentSelector, String authorSelector,
        String publishTimeSelector, String datePattern, String removeSelectors,
        boolean enabled, OffsetDateTime createdAt
) {
    public Map<String, String> options() {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "listSelector", listSelector);
        put(values, "linkSelector", linkSelector);
        put(values, "titleSelector", titleSelector);
        put(values, "contentSelector", contentSelector);
        put(values, "authorSelector", authorSelector);
        put(values, "publishTimeSelector", publishTimeSelector);
        put(values, "datePattern", datePattern);
        put(values, "removeSelectors", removeSelectors);
        return Map.copyOf(values);
    }

    private void put(Map<String, String> values, String key, String value) {
        if (value != null) {
            values.put(key, value);
        }
    }
}
