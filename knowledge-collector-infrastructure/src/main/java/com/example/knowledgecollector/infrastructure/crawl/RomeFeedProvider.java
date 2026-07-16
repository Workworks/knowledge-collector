package com.example.knowledgecollector.infrastructure.crawl;

import com.example.knowledgecollector.application.crawl.FeedEntry;
import com.example.knowledgecollector.application.crawl.FeedProvider;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import com.example.knowledgecollector.infrastructure.web.SafeWebContentClient;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class RomeFeedProvider implements FeedProvider {
    private final SafeWebContentClient client;

    public RomeFeedProvider(SafeWebContentClient client) {
        this.client = client;
    }

    @Override
    public SourceType supportedType() {
        return SourceType.RSS;
    }

    @Override
    public List<FeedEntry> fetch(CrawlSource source) {
        return parse(source);
    }

    public List<FeedEntry> parse(CrawlSource source) {
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(client.get(source)))) {
            var feed = new SyndFeedInput().build(reader);
            return feed.getEntries().stream()
                    .filter(entry -> entry.getLink() != null && !entry.getLink().isBlank())
                    .map(entry -> new FeedEntry(
                            entry.getTitle() == null ? "(无标题)" : entry.getTitle(),
                            entry.getLink(), entry.getAuthor(),
                            entry.getDescription() == null ? null : entry.getDescription().getValue(),
                            entry.getPublishedDate() == null ? null : OffsetDateTime.ofInstant(
                                    entry.getPublishedDate().toInstant(), ZoneId.systemDefault())
                    )).toList();
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Feed 解析失败", exception);
        }
    }
}
