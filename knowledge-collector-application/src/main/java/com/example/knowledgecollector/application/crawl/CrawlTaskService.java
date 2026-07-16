package com.example.knowledgecollector.application.crawl;

import com.example.knowledgecollector.application.assessment.ArticleAssessmentService;
import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.rule.SourceRuleService;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CrawlTaskService {
    private static final Logger log = LoggerFactory.getLogger(CrawlTaskService.class);
    private final CrawlSourceService sources;
    private final SourceRuleService rules;
    private final CrawlTaskGateway gateway;
    private final ArticleAssessmentService assessments;
    private final List<ContentSourceProvider> providers;

    public CrawlTaskService(CrawlSourceService sources, SourceRuleService rules,
                            CrawlTaskGateway gateway, ArticleAssessmentService assessments,
                            List<ContentSourceProvider> providers) {
        this.sources = sources;
        this.rules = rules;
        this.gateway = gateway;
        this.assessments = assessments;
        this.providers = providers;
    }

    public CrawlTaskView runSource(long sourceId) {
        return runSource(sourceId, "MANUAL_SOURCE", null);
    }

    public CrawlTaskView runSource(long sourceId, String triggerType, Long retryOfTaskId) {
        CrawlSource source = sources.get(sourceId);
        if (!source.enabled()) {
            throw new BusinessRuleException("SOURCE-DISABLED", "采集源已停用");
        }
        ContentSourceProvider provider = provider(source.type());
        CrawlTaskView task = gateway.create(source, triggerType, retryOfTaskId);
        long started = System.currentTimeMillis();
        MDC.put("taskId", task.id().toString());
        MDC.put("sourceId", source.id().toString());
        try {
            gateway.running(task.id());
            gateway.heartbeat(task.id());
            log.info("stage=DISCOVER provider={} task started", provider.getClass().getSimpleName());
            var result = provider.fetch(request(source));
            int created = 0;
            int duplicates = 0;
            for (var entry : result.items()) {
                gateway.heartbeat(task.id());
                log.debug("stage=PARSE url={}", entry.url());
                CrawlTaskGateway.SaveResult saved = gateway.saveEntry(task.id(), source, entry);
                MDC.put("articleId", saved.articleId().toString());
                log.debug("stage={} articleId={}", saved.created() ? "SAVE" : "DEDUPLICATE", saved.articleId());
                MDC.remove("articleId");
                if (saved.created()) {
                    log.debug("stage=CLASSIFY articleId={}", saved.articleId());
                    assessments.assess(saved.articleId());
                    created++;
                } else {
                    duplicates++;
                }
            }
            gateway.success(task.id(), result.items().size(), created, duplicates,
                    System.currentTimeMillis() - started);
        } catch (Exception exception) {
            log.error("stage=FETCH retryable=false retryCount=0 crawl failed", exception);
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
        try {
            return provider(source.type()).fetch(request(source)).items().size();
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("NETWORK-DISABLED")) {
                throw new BusinessRuleException("NETWORK-DISABLED", "当前环境已禁用外部网络请求");
            }
            throw exception;
        }
    }

    private ContentSourceProvider.FetchRequest request(CrawlSource source) {
        Map<String, String> options = source.type() == SourceType.HTML_LIST
                ? rules.active(source.id()).options() : Map.of();
        return new ContentSourceProvider.FetchRequest(
                source.type().name(), source.homeUrl(), source.feedUrl(), source.language(),
                source.charset(), source.userAgent(), source.timeoutSeconds(), options);
    }

    private ContentSourceProvider provider(SourceType type) {
        return providers.stream().filter(candidate -> candidate.supports(type.name())).findFirst()
                .orElseThrow(() -> new BusinessRuleException("PROVIDER-NOT-AVAILABLE",
                        "未找到 " + type + " 对应的能力 Provider"));
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

    public CrawlTaskView retry(long id) {
        CrawlTaskView original = get(id);
        if (!"FAILED".equals(original.status())) {
            throw new BusinessRuleException("TASK-RETRY-NOT-ALLOWED", "只有失败任务可以重试");
        }
        return runSource(original.sourceId(), "RETRY", original.id());
    }

    public CrawlTaskView cancel(long id) {
        get(id);
        if (!gateway.requestCancel(id)) {
            throw new BusinessRuleException("TASK-CANCEL-NOT-ALLOWED", "只有尚未开始的任务可以取消");
        }
        return get(id);
    }

    public int recoverStaleTasks(java.time.OffsetDateTime cutoff) {
        return gateway.expireStale(cutoff);
    }
}
