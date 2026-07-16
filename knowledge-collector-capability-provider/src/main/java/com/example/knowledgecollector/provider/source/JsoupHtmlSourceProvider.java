package com.example.knowledgecollector.provider.source;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JsoupHtmlSourceProvider implements ContentSourceProvider {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ITEMS = 50;
    private final WebContentProvider web;

    public JsoupHtmlSourceProvider(WebContentProvider web) {
        this.web = web;
    }

    @Override
    public boolean supports(String sourceType) {
        return "HTML_LIST".equals(sourceType);
    }

    @Override
    public FetchResult fetch(FetchRequest request) {
        requireOptions(request.options(), "listSelector", "linkSelector", "titleSelector", "contentSelector");
        Document list = fetchDocument(request.entryUrl(), request);
        List<ContentItem> items = new ArrayList<>();
        for (Element row : list.select(request.options().get("listSelector"))) {
            if (items.size() >= MAX_ITEMS) {
                break;
            }
            Element link = row.selectFirst(request.options().get("linkSelector"));
            if (link == null || link.absUrl("href").isBlank()) {
                continue;
            }
            Document detail = fetchDocument(link.absUrl("href"), request);
            String title = text(detail, request.options().get("titleSelector"), row.text());
            Element content = detail.selectFirst(request.options().get("contentSelector"));
            if (content == null || content.text().isBlank()) {
                continue;
            }
            remove(content, request.options().get("removeSelectors"));
            String cleanHtml = Jsoup.clean(content.html(), detail.baseUri(),
                    Safelist.relaxed().removeTags("img").addProtocols("a", "href", "http", "https"));
            String cleanText = Jsoup.parseBodyFragment(cleanHtml, detail.baseUri()).text();
            String author = text(detail, request.options().get("authorSelector"), null);
            String summary = cleanText.length() > 300 ? cleanText.substring(0, 300) : cleanText;
            items.add(new ContentItem(title, detail.location(), author, summary,
                    parseTime(text(detail, request.options().get("publishTimeSelector"), null),
                            request.options().get("datePattern")),
                    cleanHtml, cleanText, Map.of("provider", "jsoup")));
        }
        return new FetchResult(List.copyOf(items), Map.of("provider", "jsoup"));
    }

    private Document fetchDocument(String url, FetchRequest request) {
        var response = web.get(new WebContentProvider.WebRequest(url, request.userAgent(),
                request.language(), request.timeoutSeconds(), MAX_BYTES));
        if (!response.contentType().toLowerCase().contains("html")) {
            throw new IllegalStateException("响应 Content-Type 不是 HTML");
        }
        return Jsoup.parse(new String(response.body(), Charset.forName(request.charset())),
                response.finalUrl());
    }

    private void requireOptions(Map<String, String> options, String... names) {
        for (String name : names) {
            if (options.get(name) == null || options.get(name).isBlank()) {
                throw new IllegalArgumentException("缺少采集规则：" + name);
            }
        }
    }

    private String text(Document document, String selector, String fallback) {
        if (selector == null || selector.isBlank()) {
            return fallback;
        }
        Element element = document.selectFirst(selector);
        return element == null || element.text().isBlank() ? fallback : element.text();
    }

    private void remove(Element content, String selectors) {
        if (selectors == null || selectors.isBlank()) {
            return;
        }
        for (String selector : selectors.split("\\r?\\n")) {
            if (!selector.isBlank()) {
                content.select(selector.trim()).remove();
            }
        }
    }

    private OffsetDateTime parseTime(String value, String pattern) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return pattern == null || pattern.isBlank()
                    ? OffsetDateTime.parse(value)
                    : OffsetDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
        } catch (Exception ignored) {
            return null;
        }
    }
}
