package com.example.knowledgecollector.application.operations;

import com.example.knowledgecollector.application.article.ArticleView;
import com.example.knowledgecollector.application.crawl.CrawlTaskView;

import java.util.List;

public record DashboardView(
        long articleCount, long unreadCount, long favoriteCount, long pendingReviewCount,
        long sourceCount, long enabledSourceCount, long taskCount, long failedTaskCount,
        List<CrawlTaskView> recentTasks, List<ArticleView> recentArticles,
        List<TopicCount> topicCounts, List<SourceHealth> sourceHealth
) {
    public record TopicCount(long topicId, String topicName, long articleCount) {
    }
    public record SourceHealth(long sourceId, String sourceName, long successCount,
                               long failedCount, int consecutiveFailures, String health) {
    }
}
