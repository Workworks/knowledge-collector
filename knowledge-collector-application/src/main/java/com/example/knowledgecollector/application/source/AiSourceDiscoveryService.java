package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.topic.TopicRepository;
import com.example.knowledgecollector.application.rule.SourceRuleService;
import com.example.knowledgecollector.application.rule.SourceRuleCommand;
import com.example.knowledgecollector.capability.intelligence.ConversationalIntelligenceProvider;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiSourceDiscoveryService {
    private final CrawlSourceService sources;
    private final CrawlTaskService tasks;
    private final TopicRepository topics;
    private final SourceRuleService rules;
    private final List<ConversationalIntelligenceProvider> providers;
    private final String defaultProvider;

    public AiSourceDiscoveryService(CrawlSourceService sources, CrawlTaskService tasks,
                                    TopicRepository topics, SourceRuleService rules,
                                    List<ConversationalIntelligenceProvider> providers,
                                    @Value("${knowledge-collector.ai.provider:ollama}") String defaultProvider) {
        this.sources = sources;
        this.tasks = tasks;
        this.topics = topics;
        this.rules = rules;
        this.providers = providers;
        this.defaultProvider = defaultProvider;
    }

    public SourceDiscoveryResult discover(String topic, String language, int count, String quality) {
        if (topic == null || topic.isBlank()) {
            throw new BusinessRuleException("SOURCE-DISCOVERY-TOPIC-EMPTY", "研究主题不能为空");
        }
        int wanted = Math.max(1, Math.min(count, 50));
        String normalizedLanguage = normalizeLanguage(language);
        var provider = providers.stream().filter(p -> p.id().equalsIgnoreCase(defaultProvider)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("AI-PROVIDER-NOT-AVAILABLE", "AI Provider 不可用"));
        String prompt = """
                为 Knowledge Collector 推荐公开、无需登录、长期稳定的资料采集源。
                主题：%s
                语言：%s
                数量：%d
                质量等级：%s
                只推荐你确信真实存在的 URL。优先官方机构、大学、研究机构、国际会议和知名媒体。
                每行严格输出：TYPE|NAME|HOME_URL|FEED_OR_PAGE_URL
                TYPE 只能为 RSS、ATOM、HTML_LIST、JSON_API 或 MANUAL_URL。不要 Markdown，不要解释。
                """.formatted(topic.trim(), normalizedLanguage, wanted * 2,
                quality == null || quality.isBlank() ? "推荐" : quality.trim());
        String answer = provider.chat(new ConversationalIntelligenceProvider.ChatRequest(
                List.of(new ConversationalIntelligenceProvider.ChatMessage("user", prompt)))).content();
        List<Candidate> candidates = parse(answer);
        Set<String> seen = new LinkedHashSet<>();
        List<CrawlSource> imported = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        Set<Long> topicIds = topics.findAllEnabled().stream()
                .filter(item -> item.name().equalsIgnoreCase(topic.trim()))
                .map(item -> item.id()).collect(java.util.stream.Collectors.toSet());
        for (Candidate candidate : candidates) {
            if (imported.size() >= wanted) break;
            String key = candidate.feedUrl().toLowerCase(Locale.ROOT);
            if (!seen.add(key) || sources.existsByFeedUrl(candidate.feedUrl())) continue;
            try {
                CrawlSource temporary = temporary(candidate, normalizedLanguage, topicIds);
                int entries = tasks.testCandidate(temporary);
                CrawlSource created = sources.create(command(candidate, normalizedLanguage, topicIds,
                        "AI 自动发现并验证；检查时发现 " + entries + " 条内容"));
                if (candidate.type() == SourceType.HTML_LIST) {
                    rules.create(created.id(), new SourceRuleCommand("article, .post, .item, li",
                            "a[href]", "h1", "article, main, .content, .post-content",
                            null, null, null, "script,style,nav,footer,aside", true));
                }
                imported.add(sources.updateHealth(created.id(), true,
                        "AI 自动发现，访问正常，发现 " + entries + " 条内容"));
            } catch (Exception exception) {
                rejected.add(candidate.name() + "：" + safeMessage(exception));
            }
        }
        return new SourceDiscoveryResult(wanted, candidates.size(), imported.size(), imported, rejected);
    }

    private List<Candidate> parse(String answer) {
        List<Candidate> result = new ArrayList<>();
        for (String line : answer.split("\\R")) {
            String cleaned = line.replace("`", "").replaceFirst("^[-*\\d.\\s]+", "").trim();
            String[] parts = cleaned.split("\\|", -1);
            if (parts.length != 4) continue;
            try {
                SourceType type = SourceType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
                String home = parts[2].trim();
                String feed = parts[3].trim();
                if (home.matches("https?://.+") && feed.matches("https?://.+")) {
                    result.add(new Candidate(type, parts[1].trim(), home, feed));
                }
            } catch (IllegalArgumentException ignored) {
                // 不接受模型输出的未知类型。
            }
        }
        return result;
    }

    private CrawlSource temporary(Candidate c, String language, Set<Long> topicIds) {
        OffsetDateTime now = OffsetDateTime.now();
        return new CrawlSource(null, code(c), c.name(), c.type(), c.homeUrl(), c.feedUrl(), language,
                "UTF-8", "KnowledgeCollector/1.0 (+local-admin)", 15, 1, 1000,
                true, true, false, false, true, null, null, 0,
                "UNKNOWN", null, null, null, topicIds, 0, now, now);
    }

    private CrawlSourceCommand command(Candidate c, String language, Set<Long> topicIds, String notes) {
        return new CrawlSourceCommand(code(c), c.name(), c.type(), c.homeUrl(), c.feedUrl(), language,
                "UTF-8", "KnowledgeCollector/1.0 (+local-admin)", 15, 2, 2000,
                true, true, false, false, true, notes, topicIds);
    }

    private String code(Candidate c) {
        String host = java.net.URI.create(c.homeUrl()).getHost();
        String normalized = (host == null ? "source" : host).replaceAll("[^A-Za-z0-9]", "_");
        return ("AI_" + normalized + "_" + Integer.toUnsignedString(c.feedUrl().hashCode(), 36))
                .substring(0, Math.min(64, ("AI_" + normalized + "_" + Integer.toUnsignedString(c.feedUrl().hashCode(), 36)).length()));
    }

    private String normalizeLanguage(String language) {
        if (language == null) return "zh-CN";
        return language.toLowerCase(Locale.ROOT).startsWith("en") || language.contains("英文") ? "en" : "zh-CN";
    }

    private String safeMessage(Exception exception) {
        String value = exception.getMessage();
        return value == null ? exception.getClass().getSimpleName() : value.substring(0, Math.min(180, value.length()));
    }

    private record Candidate(SourceType type, String name, String homeUrl, String feedUrl) {}
}
