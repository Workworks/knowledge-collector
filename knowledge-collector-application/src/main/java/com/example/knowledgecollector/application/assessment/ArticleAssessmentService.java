package com.example.knowledgecollector.application.assessment;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.application.topic.TopicRepository;
import com.example.knowledgecollector.domain.article.ArticleAssessmentRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleAssessmentService {
    private final ArticleService articles;
    private final CrawlSourceService sources;
    private final TopicRepository topics;
    private final ArticleAssessmentGateway gateway;

    public ArticleAssessmentService(ArticleService articles, CrawlSourceService sources,
                                    TopicRepository topics, ArticleAssessmentGateway gateway) {
        this.articles = articles;
        this.sources = sources;
        this.topics = topics;
        this.gateway = gateway;
    }

    @Transactional
    public ArticleAssessmentView assess(long articleId) {
        var article = articles.get(articleId);
        var source = sources.get(article.sourceId());
        var assessment = ArticleAssessmentRules.assess(
                new ArticleAssessmentRules.Input(article.title(), article.author(), article.summary(),
                        article.contentText(), article.originalUrl(), !article.publishTimeInferred()),
                topics.findAllEnabled(), source.topicIds());
        gateway.save(articleId, assessment);
        return gateway.get(articleId);
    }

    @Transactional(readOnly = true)
    public ArticleAssessmentView get(long articleId) {
        articles.get(articleId);
        return gateway.get(articleId);
    }
}
