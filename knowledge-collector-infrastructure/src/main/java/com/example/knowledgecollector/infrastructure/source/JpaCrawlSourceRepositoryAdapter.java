package com.example.knowledgecollector.infrastructure.source;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.source.CrawlSourceRepository;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import com.example.knowledgecollector.infrastructure.topic.SpringDataTopicRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaCrawlSourceRepositoryAdapter implements CrawlSourceRepository {

    private final SpringDataCrawlSourceRepository repository;
    private final SpringDataTopicRepository topicRepository;

    public JpaCrawlSourceRepositoryAdapter(SpringDataCrawlSourceRepository repository,
                                           SpringDataTopicRepository topicRepository) {
        this.repository = repository;
        this.topicRepository = topicRepository;
    }

    @Override
    public PageResult<CrawlSource> findPage(String keyword, SourceType type, Boolean enabled,
                                            Long topicId, int page, int size) {
        Specification<CrawlSourceJpaEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("code")), pattern)
                ));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (enabled != null) {
                predicates.add(builder.equal(root.get("enabled"), enabled));
            }
            if (topicId != null) {
                predicates.add(builder.equal(root.join("topics", JoinType.INNER).get("id"), topicId));
                query.distinct(true);
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var result = repository.findAll(specification,
                PageRequest.of(page, size, Sort.by("id").descending()));
        return new PageResult<>(result.map(this::toDomain).getContent(), page, size,
                result.getTotalElements(), result.getTotalPages(), "id,desc");
    }

    @Override
    public Optional<CrawlSource> findById(long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CrawlSource> findAllEnabled() {
        return repository.findAllByEnabledTrueOrderByIdAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByFeedUrl(String feedUrl) {
        return repository.existsByFeedUrl(feedUrl);
    }

    @Override
    public boolean existsByCode(String code, Long excludedId) {
        return excludedId == null ? repository.existsByCode(code)
                : repository.existsByCodeAndIdNot(code, excludedId);
    }

    @Override
    public CrawlSource save(CrawlSource source) {
        CrawlSourceJpaEntity entity = source.id() == null ? new CrawlSourceJpaEntity()
                : repository.findById(source.id()).orElseThrow();
        entity.code = source.code();
        entity.name = source.name();
        entity.type = source.type();
        entity.homeUrl = source.homeUrl();
        entity.feedUrl = source.feedUrl();
        entity.language = source.language();
        entity.charset = source.charset();
        entity.userAgent = source.userAgent();
        entity.timeoutSeconds = source.timeoutSeconds();
        entity.maxRetries = source.maxRetries();
        entity.requestIntervalMillis = source.requestIntervalMillis();
        entity.obeyRobots = source.obeyRobots();
        entity.fetchFullContent = source.fetchFullContent();
        entity.summaryOnly = source.summaryOnly();
        entity.saveSnapshot = source.saveSnapshot();
        entity.enabled = source.enabled();
        entity.lastSuccessAt = source.lastSuccessAt();
        entity.lastFailureAt = source.lastFailureAt();
        entity.consecutiveFailures = source.consecutiveFailures();
        entity.healthStatus = source.healthStatus();
        entity.lastHealthCheckedAt = source.lastHealthCheckedAt();
        entity.lastHealthMessage = source.lastHealthMessage();
        entity.notes = source.notes();
        entity.topics.clear();
        entity.topics.addAll(topicRepository.findAllById(source.topicIds()));
        entity.createdAt = source.createdAt();
        entity.updatedAt = source.updatedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(long id) {
        CrawlSourceJpaEntity entity = repository.findById(id).orElseThrow();
        entity.topics.clear();
        repository.saveAndFlush(entity);
        repository.delete(entity);
    }

    private CrawlSource toDomain(CrawlSourceJpaEntity entity) {
        return new CrawlSource(entity.id, entity.code, entity.name, entity.type,
                entity.homeUrl, entity.feedUrl, entity.language, entity.charset, entity.userAgent,
                entity.timeoutSeconds, entity.maxRetries, entity.requestIntervalMillis,
                entity.obeyRobots, entity.fetchFullContent, entity.summaryOnly,
                entity.saveSnapshot, entity.enabled, entity.lastSuccessAt, entity.lastFailureAt,
                entity.consecutiveFailures, entity.healthStatus, entity.lastHealthCheckedAt,
                entity.lastHealthMessage, entity.notes,
                entity.topics.stream().map(topic -> topic.getId()).collect(java.util.stream.Collectors.toSet()),
                entity.version, entity.createdAt, entity.updatedAt);
    }
}
