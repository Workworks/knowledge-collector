package com.example.knowledgecollector.application.rule;

public record SourceRuleCommand(
        String listSelector, String linkSelector, String titleSelector, String contentSelector,
        String authorSelector, String publishTimeSelector, String datePattern,
        String removeSelectors, boolean enabled
) {
}
