package com.example.knowledgecollector.application.crawl;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrawlTaskService {
    private static final Logger log = LoggerFactory.getLogger(CrawlTaskService.class);
    private final CrawlSourceService sources;
    private final CrawlTaskGateway gateway;
    private final List<FeedProvider> providers;

    public CrawlTaskService(CrawlSourceService sources, CrawlTaskGateway gateway, List<FeedProvider> providers) {
        this.sources = sources;
        this.gateway = gateway;
        this.providers = providers;
    }

    public CrawlTaskView runSource(long sourceId) {
        CrawlSource source = sources.get(sourceId);
        if (!source.enabled()) {
            throw new BusinessRuleException("SOURCE-DISABLED", "采集源已停用");
        }
        ensureFeedType(source.type());
        CrawlTaskView task = gateway.create(source);
        long started = System.currentTimeMillis();
        MDC.put("taskId", task.id().toString());
        MDC.put("sourceId", source.id().toString());
        try {
            gateway.running(task.id());
            log.info("stage=FETCH task started");
            List<FeedEntry> entries = provider(source.type()).fetch(source);
            int created = 0;
            int duplicates = 0;
            for (FeedEntry entry : entries) {
                CrawlTaskGateway.SaveResult result = gateway.saveEntry(task.id(), source, entry);
                if (result.created()) {
                    created++;
                } else {
                    duplicates++;
                }
            }
            gateway.success(task.id(), entries.size(), created, duplicates,
                    System.currentTimeMillis() - started);
        } catch (Exception exception) {
            log.error("stage=FETCH retryable=false crawl failed", exception);
            gateway.failure(task.id(), "CRAWL-FETCH-FAILED", exception.getMessage(),
                    System.currentTimeMillis() - started);
        } finally {
            MDC.remove("taskId");
            MDC.remove("sourceId");
        }
        return gateway.get(task.id());
    }

    public int testSource(long sourceId) {
        CrawlSource source = sources.get(sourceId);
        ensureFeedType(source.type());
        return provider(source.type()).fetch(source).size();
    }

    private void ensureFeedType(SourceType type) {
        if (type != SourceType.RSS && type != SourceType.ATOM) {
            throw new BusinessRuleException("PROVIDER-NOT-AVAILABLE", "当前阶段只支持 RSS 和 Atom");
        }
    }

    private FeedProvider provider(SourceType type) {
        return providers.stream().filter(candidate -> candidate.supportedType() == type).findFirst()
                .orElseThrow(() -> new BusinessRuleException("PROVIDER-NOT-AVAILABLE", "未找到对应采集器"));
    }

    public CrawlTaskView get(long id) {
        return gateway.get(id);
    }

    public PageResult<CrawlTaskView> findPage(int page, int size) {
        return gateway.findPage(page, size);
    }

    public List<?> items(long id) {
        return gateway.findItems(id);
    }
}
