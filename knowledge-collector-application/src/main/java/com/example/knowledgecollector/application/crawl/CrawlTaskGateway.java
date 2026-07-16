package com.example.knowledgecollector.application.crawl;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.domain.source.CrawlSource;
import java.util.List;
import java.time.OffsetDateTime;

public interface CrawlTaskGateway {
    CrawlTaskView create(CrawlSource source);
    CrawlTaskView create(CrawlSource source, String triggerType, Long retryOfTaskId);
    void running(long id);
    void heartbeat(long id);
    int expireStale(OffsetDateTime cutoff);
    SaveResult saveEntry(long taskId, CrawlSource source, ContentSourceProvider.ContentItem entry);
    void success(long id, int discovered, int created, int duplicates, long duration);
    void failure(long id, String code, String message, long duration);
    boolean requestCancel(long id);
    CrawlTaskView get(long id);
    PageResult<CrawlTaskView> findPage(int page, int size);
    List<?> findItems(long taskId);
    record SaveResult(boolean created, Long articleId) {}
}
