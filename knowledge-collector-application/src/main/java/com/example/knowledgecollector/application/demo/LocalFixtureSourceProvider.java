package com.example.knowledgecollector.application.demo;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
@Profile("local")
@Order(0)
public class LocalFixtureSourceProvider implements ContentSourceProvider {
    @Override
    public boolean supports(String sourceType) {
        return "JSON_API".equals(sourceType);
    }

    @Override
    public FetchResult fetch(FetchRequest request) {
        String base = request.entryUrl() == null || request.entryUrl().isBlank()
                ? "https://fixture.knowledge-collector.local/articles" : request.entryUrl();
        String content = """
                间隔重复、主动回忆和充足睡眠能够共同改善长期记忆。
                这份本地演示资料用于验证主题匹配、质量评分、收藏、阅读状态、标签和笔记流程。
                所有内容均由应用内置生成，不会访问互联网，也不会降低 URL 安全校验标准。
                """.repeat(3);
        var item = new ContentItem(
                "主动回忆与睡眠如何改善长期记忆",
                base + (base.contains("?") ? "&" : "?") + "fixture=stage8",
                "Knowledge Collector 测试员",
                "一份用于验证认知主题采集和阅读管理闭环的本地固定资料，包含主动回忆、睡眠与长期记忆。",
                OffsetDateTime.parse("2026-07-17T08:00:00+08:00"),
                "<p>间隔重复、主动回忆和充足睡眠能够共同改善长期记忆。</p>"
                        + "<p>这是不访问外部网络的本地演示内容。</p>",
                content, Map.of("fixture", "local-stage8"));
        return new FetchResult(List.of(item), Map.of("provider", "LocalFixtureSourceProvider"));
    }
}
