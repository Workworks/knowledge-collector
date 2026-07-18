package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.domain.source.CrawlSource;

import java.util.List;

public record SourceDiscoveryResult(int requested, int suggested, int imported,
                                    List<CrawlSource> sources, List<String> rejected) {
}
