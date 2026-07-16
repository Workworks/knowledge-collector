package com.example.knowledgecollector.application.rule;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.domain.source.SourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SourceRuleService {
    private final SourceRuleGateway gateway;
    private final CrawlSourceService sources;

    public SourceRuleService(SourceRuleGateway gateway, CrawlSourceService sources) {
        this.gateway = gateway;
        this.sources = sources;
    }

    @Transactional
    public SourceRuleView create(long sourceId, SourceRuleCommand command) {
        requireHtmlSource(sourceId);
        return gateway.create(sourceId, command);
    }

    @Transactional(readOnly = true)
    public SourceRuleView active(long sourceId) {
        requireHtmlSource(sourceId);
        return gateway.findActive(sourceId)
                .orElseThrow(() -> new BusinessRuleException("SOURCE-RULE-MISSING", "HTML 采集源尚未配置启用规则"));
    }

    @Transactional(readOnly = true)
    public List<SourceRuleView> versions(long sourceId) {
        requireHtmlSource(sourceId);
        return gateway.findVersions(sourceId);
    }

    @Transactional
    public SourceRuleView activate(long sourceId, long ruleId) {
        requireHtmlSource(sourceId);
        return gateway.activate(sourceId, ruleId);
    }

    private void requireHtmlSource(long sourceId) {
        if (sources.get(sourceId).type() != SourceType.HTML_LIST) {
            throw new BusinessRuleException("SOURCE-RULE-TYPE-INVALID", "只有 HTML_LIST 采集源可以配置页面规则");
        }
    }
}
