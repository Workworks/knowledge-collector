package com.example.knowledgecollector.infrastructure.topic;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.topic.TopicRepository;
import com.example.knowledgecollector.domain.topic.Topic;
import com.example.knowledgecollector.domain.topic.TopicRules;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaTopicRepositoryAdapter implements TopicRepository {

    private final SpringDataTopicRepository repository;

    public JpaTopicRepositoryAdapter(SpringDataTopicRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<Topic> findPage(String keyword, Boolean enabled, int page, int size) {
        Specification<TopicJpaEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("code")), pattern)
                ));
            }
            if (enabled != null) {
                predicates.add(builder.equal(root.get("enabled"), enabled));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var result = repository.findAll(specification,
                PageRequest.of(page, size, Sort.by("sortOrder").ascending().and(Sort.by("id"))));
        return new PageResult<>(result.map(this::toDomain).getContent(), page, size,
                result.getTotalElements(), result.getTotalPages(), "sortOrder,asc");
    }

    @Override
    public List<Topic> findAllEnabled() {
        return repository.findAllByEnabledTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<Topic> findAllById(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Topic> findById(long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code, Long excludedId) {
        return excludedId == null ? repository.existsByCode(code)
                : repository.existsByCodeAndIdNot(code, excludedId);
    }

    @Override
    public boolean existsByName(String name, Long excludedId) {
        return excludedId == null ? repository.existsByName(name)
                : repository.existsByNameAndIdNot(name, excludedId);
    }

    @Override
    public boolean hasSources(long topicId) {
        return repository.hasSources(topicId);
    }

    @Override
    public Topic save(Topic topic) {
        TopicJpaEntity entity = topic.id() == null ? new TopicJpaEntity()
                : repository.findById(topic.id()).orElseThrow();
        entity.code = topic.code();
        entity.name = topic.name();
        entity.description = topic.description();
        entity.keywords = TopicRules.serializeTerms(topic.keywords());
        entity.excludedKeywords = TopicRules.serializeTerms(topic.excludedKeywords());
        entity.color = topic.color();
        entity.icon = topic.icon();
        entity.language = topic.language();
        entity.enabled = topic.enabled();
        entity.sortOrder = topic.sortOrder();
        entity.createdAt = topic.createdAt();
        entity.updatedAt = topic.updatedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(long id) {
        repository.deleteById(id);
    }

    private Topic toDomain(TopicJpaEntity entity) {
        return new Topic(entity.id, entity.code, entity.name, entity.description,
                TopicRules.normalizeTerms(entity.keywords),
                TopicRules.normalizeTerms(entity.excludedKeywords), entity.color, entity.icon,
                entity.language, entity.enabled, entity.sortOrder, entity.version,
                entity.createdAt, entity.updatedAt);
    }
}
