package com.example.knowledgecollector.application.topic;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.exception.ConflictException;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.domain.topic.Topic;
import com.example.knowledgecollector.domain.topic.TopicRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class TopicService {

    private final TopicRepository repository;

    public TopicService(TopicRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResult<Topic> findPage(String keyword, Boolean enabled, int page, int size) {
        return repository.findPage(keyword, enabled, page, size);
    }

    @Transactional(readOnly = true)
    public List<Topic> findEnabled() {
        return repository.findAllEnabled();
    }

    @Transactional(readOnly = true)
    public Topic get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("主题不存在：" + id));
    }

    public Topic create(TopicCommand command) {
        validateUnique(command, null);
        OffsetDateTime now = OffsetDateTime.now();
        return repository.save(toTopic(null, 0, now, now, command));
    }

    public Topic update(long id, TopicCommand command) {
        Topic existing = get(id);
        validateUnique(command, id);
        return repository.save(toTopic(id, existing.version(), existing.createdAt(),
                OffsetDateTime.now(), command));
    }

    public Topic setEnabled(long id, boolean enabled) {
        Topic existing = get(id);
        return repository.save(new Topic(existing.id(), existing.code(), existing.name(),
                existing.description(), existing.keywords(), existing.excludedKeywords(),
                existing.color(), existing.icon(), existing.language(), enabled,
                existing.sortOrder(), existing.version(), existing.createdAt(), OffsetDateTime.now()));
    }

    public void delete(long id) {
        get(id);
        if (repository.hasSources(id)) {
            throw new ConflictException("TOPIC-IN-USE", "主题已关联采集源，请先解除关联或停用主题");
        }
        repository.delete(id);
    }

    private void validateUnique(TopicCommand command, Long excludedId) {
        String code = TopicRules.normalizeCode(command.code());
        if (repository.existsByCode(code, excludedId)) {
            throw new ConflictException("TOPIC-CODE-EXISTS", "主题编码已存在");
        }
        if (repository.existsByName(command.name().trim(), excludedId)) {
            throw new ConflictException("TOPIC-NAME-EXISTS", "主题名称已存在");
        }
    }

    private Topic toTopic(Long id, long version, OffsetDateTime createdAt,
                          OffsetDateTime updatedAt, TopicCommand command) {
        return new Topic(id, TopicRules.normalizeCode(command.code()), command.name().trim(),
                trimToNull(command.description()), TopicRules.normalizeTerms(command.keywords()),
                TopicRules.normalizeTerms(command.excludedKeywords()), command.color(),
                trimToNull(command.icon()), command.language(), command.enabled(),
                command.sortOrder(), version, createdAt, updatedAt);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
