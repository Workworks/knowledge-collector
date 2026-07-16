package com.example.knowledgecollector.application.reading;

import java.util.List;

public interface ArticleReadingGateway {
    ArticleReadingView get(long articleId);
    ArticleReadingView updateState(long articleId, String readingStatus, Boolean favorite, Boolean archived);
    ArticleReadingView saveNote(long articleId, String content);
    ArticleReadingView replaceTags(long articleId, List<String> tagNames);
    List<TagView> tags();
}
