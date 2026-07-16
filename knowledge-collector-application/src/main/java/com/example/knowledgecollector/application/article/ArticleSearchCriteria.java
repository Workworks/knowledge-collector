package com.example.knowledgecollector.application.article;

public record ArticleSearchCriteria(
        String keyword, Long sourceId, String reviewStatus, Integer minQuality, Long topicId,
        String readingStatus, Boolean favorite, Boolean archived, Long tagId,
        String sort, int page, int size
) {
    public static ArticleSearchCriteria basic(String keyword, Long sourceId, int page, int size) {
        return new ArticleSearchCriteria(keyword, sourceId, null, null, null,
                null, null, false, null, "publishTime,desc", page, size);
    }
}
