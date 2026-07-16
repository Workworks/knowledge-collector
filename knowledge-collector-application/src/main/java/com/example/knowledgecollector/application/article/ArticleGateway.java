package com.example.knowledgecollector.application.article;

import com.example.knowledgecollector.application.common.PageResult;

public interface ArticleGateway {
    PageResult<ArticleView> findPage(String keyword, Long sourceId, int page, int size);
    PageResult<ArticleView> findPage(String keyword, Long sourceId, String reviewStatus,
                                     Integer minQuality, Long topicId, int page, int size);
    ArticleView get(long id);
}
