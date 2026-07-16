package com.example.knowledgecollector.application.demo;

import com.example.knowledgecollector.application.source.CrawlSourceCommand;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.application.rule.SourceRuleCommand;
import com.example.knowledgecollector.application.rule.SourceRuleService;
import com.example.knowledgecollector.application.topic.TopicCommand;
import com.example.knowledgecollector.application.topic.TopicService;
import com.example.knowledgecollector.domain.source.SourceType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Profile("local")
@Transactional
public class DemoDataService {

    private final TopicService topics;
    private final CrawlSourceService sources;
    private final SourceRuleService rules;

    public DemoDataService(TopicService topics, CrawlSourceService sources, SourceRuleService rules) {
        this.topics = topics;
        this.sources = sources;
        this.rules = rules;
    }

    public void initialize() {
        long science = ensureTopic("DEMO_SCIENCE", "科学", "科学、研究与发现", 10);
        long cognition = ensureTopic("DEMO_COGNITION", "认知", "认知科学与思维", 20);
        long news = ensureTopic("DEMO_NEWS", "新闻", "公开新闻资讯", 30);
        ensureSource("DEMO_RSS", "演示 RSS 来源", SourceType.RSS,
                "https://example.com/feed.xml", Set.of(science, news));
        long htmlSource = ensureSource("DEMO_HTML", "演示 HTML 来源", SourceType.HTML_LIST,
                "https://example.com/articles", Set.of(cognition));
        if (rules.versions(htmlSource).isEmpty()) {
            rules.create(htmlSource, new SourceRuleCommand(
                    ".article-item", "a", "h1", "article", ".author", "time",
                    "", ".advertisement\nscript\niframe", true));
        }
    }

    public void clear() {
        var sourcePage = sources.findPage("DEMO_", null, null, null, 0, 100);
        sourcePage.content().forEach(source -> sources.delete(source.id()));
        var topicPage = topics.findPage("DEMO_", null, 0, 100);
        topicPage.content().forEach(topic -> topics.delete(topic.id()));
    }

    private long ensureTopic(String code, String name, String keywords, int sortOrder) {
        var existing = topics.findPage(code, null, 0, 10).content().stream()
                .filter(topic -> topic.code().equals(code)).findFirst();
        if (existing.isPresent()) {
            return existing.get().id();
        }
        return topics.create(new TopicCommand(code, name, "本地测试演示数据",
                keywords, "广告", "#2563EB", "book", "zh-CN", true, sortOrder)).id();
    }

    private long ensureSource(String code, String name, SourceType type,
                              String feedUrl, Set<Long> topicIds) {
        var existing = sources.findPage(code, null, null, null, 0, 10).content().stream()
                .filter(source -> source.code().equals(code)).findFirst();
        if (existing.isPresent()) {
            return existing.get().id();
        }
        return sources.create(new CrawlSourceCommand(code, name, type, "https://example.com",
                    feedUrl, "zh-CN", "UTF-8", "KnowledgeCollector-Demo/1.0",
                    15, 1, 2000, true, true, false,
                    false, true, "LOCAL_DEMO_DATA", new LinkedHashSet<>(topicIds))).id();
    }
}
