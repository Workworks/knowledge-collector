package com.example.knowledgecollector.application.topic;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.domain.topic.Topic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TopicRepository {

    PageResult<Topic> findPage(String keyword, Boolean enabled, int page, int size);

    List<Topic> findAllEnabled();

    List<Topic> findAllById(Collection<Long> ids);

    Optional<Topic> findById(long id);

    boolean existsByCode(String code, Long excludedId);

    boolean existsByName(String name, Long excludedId);

    boolean hasSources(long topicId);

    Topic save(Topic topic);

    void delete(long id);
}
