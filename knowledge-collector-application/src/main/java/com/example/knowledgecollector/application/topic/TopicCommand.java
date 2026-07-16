package com.example.knowledgecollector.application.topic;

public record TopicCommand(
        String code,
        String name,
        String description,
        String keywords,
        String excludedKeywords,
        String color,
        String icon,
        String language,
        boolean enabled,
        int sortOrder
) {
}
