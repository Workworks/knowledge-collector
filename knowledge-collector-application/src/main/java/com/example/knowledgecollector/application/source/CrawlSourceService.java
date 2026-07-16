package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.exception.ConflictException;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.topic.TopicRepository;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceRules;
import com.example.knowledgecollector.domain.source.SourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
@Transactional
public class CrawlSourceService {

    private final CrawlSourceRepository repository;
    private final TopicRepository topicRepository;

    public CrawlSourceService(CrawlSourceRepository repository, TopicRepository topicRepository) {
        this.repository = repository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<CrawlSource> findPage(String keyword, SourceType type, Boolean enabled,
                                            Long topicId, int page, int size) {
        return repository.findPage(keyword, type, enabled, topicId, page, size);
    }

    @Transactional(readOnly = true)
    public CrawlSource get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("采集源不存在：" + id));
    }

    public CrawlSource create(CrawlSourceCommand command) {
        validate(command, null);
        OffsetDateTime now = OffsetDateTime.now();
        return repository.save(toSource(null, 0, now, now, command, null));
    }

    public CrawlSource update(long id, CrawlSourceCommand command) {
        CrawlSource existing = get(id);
        validate(command, id);
        return repository.save(toSource(id, existing.version(), existing.createdAt(),
                OffsetDateTime.now(), command, existing));
    }

    public CrawlSource setEnabled(long id, boolean enabled) {
        CrawlSource existing = get(id);
        return repository.save(new CrawlSource(existing.id(), existing.code(), existing.name(),
                existing.type(), existing.homeUrl(), existing.feedUrl(), existing.language(),
                existing.charset(), existing.userAgent(), existing.timeoutSeconds(),
                existing.maxRetries(), existing.requestIntervalMillis(), existing.obeyRobots(),
                existing.fetchFullContent(), existing.summaryOnly(), existing.saveSnapshot(),
                enabled, existing.lastSuccessAt(), existing.lastFailureAt(),
                existing.consecutiveFailures(), existing.notes(), existing.topicIds(),
                existing.version(), existing.createdAt(), OffsetDateTime.now()));
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    public void assertTestingAvailable(long id) {
        get(id);
        throw new BusinessRuleException("SOURCE-TEST-NOT-AVAILABLE",
                "来源连通性测试将在 Stage 5 由对应采集 Provider 实现");
    }

    private void validate(CrawlSourceCommand command, Long excludedId) {
        String code = SourceRules.normalizeCode(command.code());
        if (repository.existsByCode(code, excludedId)) {
            throw new ConflictException("SOURCE-CODE-EXISTS", "采集源编码已存在");
        }
        if (!SourceRules.isHttpUrl(command.homeUrl()) || !SourceRules.isHttpUrl(command.feedUrl())) {
            throw new BusinessRuleException("SOURCE-URL-INVALID", "首页地址和订阅地址只允许 HTTP/HTTPS");
        }
        Set<Long> topicIds = command.topicIds() == null ? Set.of() : command.topicIds();
        if (topicRepository.findAllById(topicIds).size() != topicIds.size()) {
            throw new BusinessRuleException("SOURCE-TOPIC-INVALID", "关联主题包含不存在的记录");
        }
    }

    private CrawlSource toSource(Long id, long version, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt, CrawlSourceCommand command,
                                 CrawlSource existing) {
        return new CrawlSource(id, SourceRules.normalizeCode(command.code()), command.name().trim(),
                command.type(), trimToNull(command.homeUrl()), trimToNull(command.feedUrl()),
                command.language(), command.charset(), command.userAgent(),
                command.timeoutSeconds(), command.maxRetries(), command.requestIntervalMillis(),
                command.obeyRobots(), command.fetchFullContent(), command.summaryOnly(),
                command.saveSnapshot(), command.enabled(),
                existing == null ? null : existing.lastSuccessAt(),
                existing == null ? null : existing.lastFailureAt(),
                existing == null ? 0 : existing.consecutiveFailures(), trimToNull(command.notes()),
                command.topicIds() == null ? Set.of() : Set.copyOf(command.topicIds()),
                version, createdAt, updatedAt);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
