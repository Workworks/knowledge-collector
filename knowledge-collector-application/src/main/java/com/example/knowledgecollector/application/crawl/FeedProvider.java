package com.example.knowledgecollector.application.crawl;

import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import java.util.List;

public interface FeedProvider {
    SourceType supportedType();
    List<FeedEntry> fetch(CrawlSource source);
}
