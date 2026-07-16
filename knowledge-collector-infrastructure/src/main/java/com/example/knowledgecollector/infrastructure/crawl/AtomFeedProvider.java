package com.example.knowledgecollector.infrastructure.crawl;

import com.example.knowledgecollector.application.crawl.FeedEntry;
import com.example.knowledgecollector.application.crawl.FeedProvider;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AtomFeedProvider implements FeedProvider {
    private final RomeFeedProvider delegate;
    public AtomFeedProvider(RomeFeedProvider delegate) { this.delegate = delegate; }
    @Override public SourceType supportedType() { return SourceType.ATOM; }
    @Override public List<FeedEntry> fetch(CrawlSource source) { return delegate.parse(source); }
}
