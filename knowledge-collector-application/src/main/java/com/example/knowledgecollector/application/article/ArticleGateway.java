package com.example.knowledgecollector.application.article;

import com.example.knowledgecollector.application.common.PageResult;

public interface ArticleGateway {
    PageResult<ArticleView> findPage(ArticleSearchCriteria criteria);
    ArticleView get(long id);
}
