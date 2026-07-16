package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;

import java.util.Optional;

public interface CrawlSourceRepository {

    PageResult<CrawlSource> findPage(String keyword, SourceType type, Boolean enabled,
                                     Long topicId, int page, int size);

    Optional<CrawlSource> findById(long id);

    boolean existsByCode(String code, Long excludedId);

    CrawlSource save(CrawlSource source);

    void delete(long id);
}
