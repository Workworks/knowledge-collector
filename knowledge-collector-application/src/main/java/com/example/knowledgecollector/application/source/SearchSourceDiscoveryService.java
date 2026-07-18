package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.application.capability.ThirdPartyCapabilityService;
import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.rule.SourceRuleCommand;
import com.example.knowledgecollector.application.rule.SourceRuleService;
import com.example.knowledgecollector.application.topic.TopicRepository;
import com.example.knowledgecollector.capability.source.SourceDiscoveryProvider;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SearchSourceDiscoveryService {
    private final SourceDiscoveryGateway gateway;
    private final List<SourceDiscoveryProvider> providers;
    private final ThirdPartyCapabilityService capabilities;
    private final CrawlSourceService sources;
    private final CrawlTaskService tasks;
    private final TopicRepository topics;
    private final SourceRuleService rules;

    public SearchSourceDiscoveryService(SourceDiscoveryGateway gateway, List<SourceDiscoveryProvider> providers,
            ThirdPartyCapabilityService capabilities, CrawlSourceService sources, CrawlTaskService tasks,
            TopicRepository topics, SourceRuleService rules) {
        this.gateway = gateway; this.providers = providers; this.capabilities = capabilities;
        this.sources = sources; this.tasks = tasks; this.topics = topics; this.rules = rules;
    }

    public List<DiscoveryCandidateView> discover(String topic, String language, int count,
                                                  String sourceType, String quality) {
        if (topic == null || topic.isBlank()) throw new BusinessRuleException("DISCOVERY-TOPIC-EMPTY", "主题不能为空");
        String providerId = capabilities.defaultProvider("SEARCH");
        SourceDiscoveryProvider provider = providers.stream().filter(p -> p.id().equalsIgnoreCase(providerId)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("DISCOVERY-PROVIDER-NOT-FOUND", "未加载搜索 Provider：" + providerId));
        var request = new SourceDiscoveryProvider.Request(topic.trim(), language, Math.max(1, Math.min(count, 50)), sourceType, quality);
        List<SourceDiscoveryProvider.Candidate> found = capabilities.invoke(providerId, "DISCOVER_SOURCES", "采集源自动发现",
                "主题=" + topic + "，数量=" + request.count(), () -> provider.discover(request), value -> "发现 " + value.size() + " 个候选源");
        return gateway.replace(providerId, topic.trim(), found);
    }
    public List<DiscoveryCandidateView> list() { return gateway.list(); }

    public DiscoveryCandidateView validate(long id) {
        DiscoveryCandidateView candidate = gateway.get(id);
        try {
            int entries = capabilities.invoke(candidate.providerId(), "VALIDATE_SOURCE", "采集源候选验证",
                    candidate.collectionUrl(), () -> tasks.testCandidate(temporary(candidate)), count -> "访问正常，发现 " + count + " 条内容");
            return gateway.validation(id, true, "访问正常，发现 " + entries + " 条内容");
        } catch (Exception exception) {
            return gateway.validation(id, false, safe(exception));
        }
    }
    public List<DiscoveryCandidateView> validate(List<Long> ids) { return ids.stream().map(this::validate).toList(); }

    public DiscoveryCandidateView importCandidate(long id) {
        DiscoveryCandidateView candidate = gateway.get(id);
        if (!"VERIFIED".equals(candidate.validationStatus()))
            throw new BusinessRuleException("DISCOVERY-NOT-VERIFIED", "候选源验证通过后才能导入");
        if (candidate.importedSourceId() != null) return candidate;
        if (sources.existsByFeedUrl(candidate.collectionUrl()))
            throw new BusinessRuleException("DISCOVERY-SOURCE-EXISTS", "该采集地址已经存在");
        Set<Long> topicIds = topics.findAllEnabled().stream().filter(t -> t.name().equalsIgnoreCase(candidate.topic()))
                .map(t -> t.id()).collect(java.util.stream.Collectors.toSet());
        SourceType type = SourceType.valueOf(candidate.sourceType());
        CrawlSource created = sources.create(new CrawlSourceCommand(code(candidate), candidate.name(), type,
                candidate.websiteUrl(), candidate.collectionUrl(), candidate.language(), "UTF-8",
                "KnowledgeCollector/1.0 (+local-admin)", 15, 2, 2000, true, true,
                false, false, true, "SearXNG 自动发现；可靠性 " + candidate.reliabilityScore() + " 分", topicIds));
        if (type == SourceType.HTML_LIST) rules.create(created.id(), new SourceRuleCommand("article, .post, .item, li",
                "a[href]", "h1", "article, main, .content, .post-content", null, null, null,
                "script,style,nav,footer,aside", true));
        sources.updateHealth(created.id(), true, candidate.validationMessage());
        return gateway.imported(id, created.id());
    }
    public List<DiscoveryCandidateView> importCandidates(List<Long> ids) { return ids.stream().map(this::importCandidate).toList(); }
    public DiscoveryCandidateView ignore(long id) { return gateway.ignored(id); }

    private CrawlSource temporary(DiscoveryCandidateView c) {
        OffsetDateTime now = OffsetDateTime.now();
        return new CrawlSource(null, code(c), c.name(), SourceType.valueOf(c.sourceType()), c.websiteUrl(),
                c.collectionUrl(), c.language(), "UTF-8", "KnowledgeCollector/1.0 (+local-admin)",
                15, 1, 500, true, true, false, false, true, null, null, 0,
                "UNKNOWN", null, null, null, Set.of(), 0, now, now);
    }
    private String code(DiscoveryCandidateView c) {
        String host = java.net.URI.create(c.websiteUrl()).getHost();
        String value = "SX_" + (host == null ? "source" : host).replaceAll("[^A-Za-z0-9]", "_") + "_" + Long.toString(c.id(), 36);
        return value.substring(0, Math.min(64, value.length())).toUpperCase(Locale.ROOT);
    }
    private String safe(Exception e) { String m=e.getMessage(); return m==null?e.getClass().getSimpleName():m.substring(0,Math.min(1000,m.length())); }
}
