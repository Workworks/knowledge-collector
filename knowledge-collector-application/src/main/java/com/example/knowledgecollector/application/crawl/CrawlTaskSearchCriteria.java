package com.example.knowledgecollector.application.crawl;

import java.time.OffsetDateTime;

public record CrawlTaskSearchCriteria(String keyword, Long sourceId, String status,
                                      String triggerType, OffsetDateTime createdFrom,
                                      OffsetDateTime createdTo, int page, int size) {
}
