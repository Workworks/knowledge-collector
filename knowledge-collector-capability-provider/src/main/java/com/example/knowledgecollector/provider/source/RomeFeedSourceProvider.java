package com.example.knowledgecollector.provider.source;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RomeFeedSourceProvider implements ContentSourceProvider {
    private static final Logger log = LoggerFactory.getLogger(RomeFeedSourceProvider.class);
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ITEMS = 50;
    private final WebContentProvider web;
    private final HtmlArticleContentExtractor extractor;

    public RomeFeedSourceProvider(WebContentProvider web, HtmlArticleContentExtractor extractor) {
        this.web = web;
        this.extractor = extractor;
    }

    @Override
    public boolean supports(String sourceType) {
        return "RSS".equals(sourceType) || "ATOM".equals(sourceType);
    }

    @Override
    public FetchResult fetch(FetchRequest request) {
        var response = web.get(new WebContentProvider.WebRequest(request.entryUrl(), request.userAgent(),
                request.language(), request.timeoutSeconds(), MAX_BYTES));
        String type = response.contentType().toLowerCase();
        if (!(type.contains("xml") || type.contains("rss") || type.contains("atom"))) {
            throw new IllegalStateException("响应 Content-Type 不是 Feed/XML");
        }
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(response.body()))) {
            var feed = new SyndFeedInput().build(reader);
            List<ContentItem> items = new ArrayList<>();
            for (var entry : feed.getEntries()) {
                if (items.size() >= MAX_ITEMS) {
                    break;
                }
                if (entry.getLink() == null || entry.getLink().isBlank()) {
                    continue;
                }
                String embeddedHtml = entry.getContents().isEmpty()
                        ? null : entry.getContents().get(0).getValue();
                var embedded = extractor.sanitize(embeddedHtml, entry.getLink());
                String summary = entry.getDescription() == null ? null
                        : extractor.plainText(entry.getDescription().getValue());
                if ((summary == null || summary.isBlank()) && embedded.present()) {
                    summary = abbreviate(embedded.text(), 300);
                }

                HtmlArticleContentExtractor.ExtractedContent content =
                        request.fetchFullContent() && !request.summaryOnly()
                                ? embedded : HtmlArticleContentExtractor.ExtractedContent.empty();
                String contentOrigin = embedded.present() ? "feed" : "none";
                if (request.fetchFullContent() && !request.summaryOnly()
                        && (!embedded.present() || embedded.text().length() < 200)) {
                    try {
                        var detail = web.get(new WebContentProvider.WebRequest(entry.getLink(),
                                request.userAgent(), request.language(), request.timeoutSeconds(), MAX_BYTES));
                        if (detail.contentType().toLowerCase().contains("html")) {
                            var extracted = extractor.extract(detail.body(), detail.contentType(),
                                    request.charset(), detail.finalUrl());
                            if (extracted.present()) {
                                content = extracted;
                                contentOrigin = "article-page";
                            }
                        }
                    } catch (Exception exception) {
                        log.warn("RSS article body fetch failed; falling back to feed content url={} reason={}",
                                entry.getLink(), exception.getMessage());
                    }
                }
                items.add(new ContentItem(
                        entry.getTitle() == null ? "(无标题)" : entry.getTitle(),
                        entry.getLink(), entry.getAuthor(), summary,
                        entry.getPublishedDate() == null ? null : OffsetDateTime.ofInstant(
                                entry.getPublishedDate().toInstant(), ZoneId.systemDefault()),
                        content.html(), content.text(),
                        Map.of("provider", "rome", "contentOrigin", contentOrigin)
                ));
            }
            return new FetchResult(List.copyOf(items), Map.of("finalUrl", response.finalUrl()));
        } catch (Exception exception) {
            throw new IllegalStateException("Feed 解析失败", exception);
        }
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
