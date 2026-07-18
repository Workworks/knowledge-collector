package com.example.knowledgecollector.provider.extraction;

import com.example.knowledgecollector.capability.extraction.ContentExtractionProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import com.example.knowledgecollector.provider.source.HtmlArticleContentExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class DirectContentExtractionProvider implements ContentExtractionProvider {
    private final WebContentProvider web;
    private final HtmlArticleContentExtractor extractor;
    public DirectContentExtractionProvider(WebContentProvider web, HtmlArticleContentExtractor extractor) {
        this.web = web; this.extractor = extractor;
    }
    @Override public String id() { return "direct"; }
    @Override public ExtractionResult extract(ExtractionRequest request) {
        var response = web.get(new WebContentProvider.WebRequest(request.url(), "KnowledgeCollector/1.0", "zh-CN",
                Math.max(5, request.timeoutSeconds()), 10 * 1024 * 1024));
        if (response.status() / 100 != 2) throw new IllegalStateException("HTTP-" + response.status());
        String raw = new String(response.body(), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(raw, response.finalUrl());
        var content = extractor.extract(response.body(), response.contentType(), "UTF-8", response.finalUrl());
        if (!content.present()) throw new IllegalStateException("ARTICLE-CONTENT-NOT-FOUND: 未提取到可用正文");
        String author = meta(document, "meta[name=author]");
        String published = meta(document, "meta[property=article:published_time]");
        return new ExtractionResult(response.finalUrl(), document.title(), author, date(published),
                content.html(), content.text(), raw, null, Map.of("provider", id()));
    }
    private String meta(Document document, String selector) { var node=document.selectFirst(selector); return node==null?null:node.attr("content"); }
    private OffsetDateTime date(String value) { try { return value==null||value.isBlank()?null:OffsetDateTime.parse(value); } catch(Exception ignored){return null;} }
}
