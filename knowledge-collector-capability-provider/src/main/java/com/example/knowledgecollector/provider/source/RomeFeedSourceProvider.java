package com.example.knowledgecollector.provider.source;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
public class RomeFeedSourceProvider implements ContentSourceProvider {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final WebContentProvider web;

    public RomeFeedSourceProvider(WebContentProvider web) {
        this.web = web;
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
            List<ContentItem> items = feed.getEntries().stream()
                    .filter(entry -> entry.getLink() != null && !entry.getLink().isBlank())
                    .map(entry -> new ContentItem(
                            entry.getTitle() == null ? "(无标题)" : entry.getTitle(),
                            entry.getLink(), entry.getAuthor(),
                            entry.getDescription() == null ? null : entry.getDescription().getValue(),
                            entry.getPublishedDate() == null ? null : OffsetDateTime.ofInstant(
                                    entry.getPublishedDate().toInstant(), ZoneId.systemDefault()),
                            null, null, Map.of("provider", "rome")
                    )).toList();
            return new FetchResult(items, Map.of("finalUrl", response.finalUrl()));
        } catch (Exception exception) {
            throw new IllegalStateException("Feed 解析失败", exception);
        }
    }
}
