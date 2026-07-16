package com.example.knowledgecollector.web.rule;

import com.example.knowledgecollector.application.rule.SourceRuleCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceRuleRequest(
        @NotBlank @Size(max = 500) String listSelector,
        @NotBlank @Size(max = 500) String linkSelector,
        @NotBlank @Size(max = 500) String titleSelector,
        @NotBlank @Size(max = 500) String contentSelector,
        @Size(max = 500) String authorSelector,
        @Size(max = 500) String publishTimeSelector,
        @Size(max = 128) String datePattern,
        @Size(max = 5000) String removeSelectors,
        boolean enabled
) {
    public SourceRuleCommand toCommand() {
        return new SourceRuleCommand(listSelector, linkSelector, titleSelector, contentSelector,
                authorSelector, publishTimeSelector, datePattern, removeSelectors, enabled);
    }
}
