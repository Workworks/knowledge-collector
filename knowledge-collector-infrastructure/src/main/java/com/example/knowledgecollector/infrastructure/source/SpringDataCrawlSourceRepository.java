package com.example.knowledgecollector.infrastructure.source;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SpringDataCrawlSourceRepository
        extends JpaRepository<CrawlSourceJpaEntity, Long>,
        JpaSpecificationExecutor<CrawlSourceJpaEntity> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByFeedUrl(String feedUrl);

    List<CrawlSourceJpaEntity> findAllByEnabledTrueOrderByIdAsc();
}
