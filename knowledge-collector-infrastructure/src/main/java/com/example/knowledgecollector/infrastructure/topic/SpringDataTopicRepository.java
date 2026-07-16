package com.example.knowledgecollector.infrastructure.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataTopicRepository
        extends JpaRepository<TopicJpaEntity, Long>, JpaSpecificationExecutor<TopicJpaEntity> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<TopicJpaEntity> findAllByEnabledTrueOrderBySortOrderAscNameAsc();

    @Query("select count(s) > 0 from CrawlSourceJpaEntity s join s.topics t where t.id = :topicId")
    boolean hasSources(@Param("topicId") long topicId);
}
