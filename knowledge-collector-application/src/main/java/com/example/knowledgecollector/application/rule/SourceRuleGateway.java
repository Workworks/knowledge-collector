package com.example.knowledgecollector.application.rule;

import java.util.List;
import java.util.Optional;

public interface SourceRuleGateway {
    SourceRuleView create(long sourceId, SourceRuleCommand command);
    Optional<SourceRuleView> findActive(long sourceId);
    List<SourceRuleView> findVersions(long sourceId);
    SourceRuleView activate(long sourceId, long ruleId);
}
