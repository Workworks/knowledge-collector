package com.example.knowledgecollector.application.article;

import com.example.knowledgecollector.application.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArticleService {
    private final ArticleGateway gateway;

    public ArticleService(ArticleGateway gateway) {
        this.gateway = gateway;
    }

    public PageResult<ArticleView> findPage(String keyword, Long sourceId, int page, int size) {
        return gateway.findPage(ArticleSearchCriteria.basic(keyword, sourceId, page, size));
    }

    public PageResult<ArticleView> findPage(String keyword, Long sourceId, String reviewStatus,
                                            Integer minQuality, Long topicId, int page, int size) {
        return gateway.findPage(new ArticleSearchCriteria(keyword, sourceId, reviewStatus,
                minQuality, topicId, null, null, null, null,
                "publishTime,desc", page, size));
    }

    public PageResult<ArticleView> findPage(ArticleSearchCriteria criteria) {
        return gateway.findPage(criteria);
    }

    public ArticleView get(long id) {
        return gateway.get(id);
    }
}
