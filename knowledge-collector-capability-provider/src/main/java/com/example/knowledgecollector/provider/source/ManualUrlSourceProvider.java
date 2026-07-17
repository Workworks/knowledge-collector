package com.example.knowledgecollector.provider.source;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** 直接采集单个网页的 MANUAL_URL Provider。 */
@Component
public class ManualUrlSourceProvider implements ContentSourceProvider {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final WebContentProvider web;
    private final HtmlArticleContentExtractor extractor;

    public ManualUrlSourceProvider(WebContentProvider web, HtmlArticleContentExtractor extractor) {
        this.web = web;
        this.extractor = extractor;
    }

    @Override
    public boolean supports(String sourceType) {
        return "MANUAL_URL".equals(sourceType);
    }

    @Override
    public FetchResult fetch(FetchRequest request) {
        String url = firstNonBlank(request.entryUrl(), request.homeUrl());
        if (url == null) {
            throw new IllegalArgumentException("MANUAL_URL 必须配置采集地址");
        }
        var response = web.get(new WebContentProvider.WebRequest(url, request.userAgent(),
                request.language(), request.timeoutSeconds(), MAX_BYTES));
        if (!response.contentType().toLowerCase().contains("html")) {
            throw new IllegalStateException("MANUAL_URL 响应 Content-Type 不是 HTML");
        }
        var content = extractor.extract(response.body(), response.contentType(),
                request.charset(), response.finalUrl());
        if (!content.present()) {
            throw new IllegalStateException("MANUAL_URL 页面未提取到可用正文");
        }
        Document document = Jsoup.parse(new String(response.body(), Charset.forName(request.charset())),
                response.finalUrl());
        String title = firstNonBlank(document.title(), response.finalUrl());
        String author = meta(document, "meta[name=author]", "content");
        String summary = meta(document, "meta[name=description]", "content");
        if (summary == null) {
            summary = abbreviate(content.text(), 300);
        }
        ContentItem item = new ContentItem(title, response.finalUrl(), author, summary,
                OffsetDateTime.now(), content.html(), content.text(),
                Map.of("provider", "manual-url", "contentOrigin", "article-page"));
        return new FetchResult(List.of(item), Map.of("provider", "manual-url",
                "finalUrl", response.finalUrl()));
    }

    private String meta(Document document, String selector, String attribute) {
        var element = document.selectFirst(selector);
        return element == null ? null : firstNonBlank(element.attr(attribute));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
