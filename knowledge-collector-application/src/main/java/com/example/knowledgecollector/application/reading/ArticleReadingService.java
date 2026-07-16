package com.example.knowledgecollector.application.reading;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class ArticleReadingService {
    private static final List<String> STATUSES = List.of("UNREAD", "READ", "ARCHIVED", "IGNORED");
    private final ArticleService articles;
    private final ArticleReadingGateway gateway;

    public ArticleReadingService(ArticleService articles, ArticleReadingGateway gateway) {
        this.articles = articles;
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public ArticleReadingView get(long articleId) {
        articles.get(articleId);
        return gateway.get(articleId);
    }

    @Transactional
    public ArticleReadingView updateState(long articleId, String readingStatus,
                                          Boolean favorite, Boolean archived) {
        articles.get(articleId);
        String normalized = readingStatus == null ? null : readingStatus.trim().toUpperCase(Locale.ROOT);
        if (normalized != null && !STATUSES.contains(normalized)) {
            throw new BusinessRuleException("ARTICLE-READING-STATUS",
                    "阅读状态必须是 UNREAD、READ、ARCHIVED 或 IGNORED");
        }
        if ("ARCHIVED".equals(normalized)) {
            archived = true;
        } else if (normalized != null && Boolean.TRUE.equals(archived)) {
            normalized = "ARCHIVED";
        } else if (normalized != null && archived == null) {
            archived = false;
        }
        return gateway.updateState(articleId, normalized, favorite, archived);
    }

    @Transactional
    public ArticleReadingView saveNote(long articleId, String content) {
        articles.get(articleId);
        String value = content == null ? "" : content.trim();
        if (value.length() > 10000) {
            throw new BusinessRuleException("ARTICLE-NOTE-LENGTH", "笔记不能超过 10000 个字符");
        }
        return gateway.saveNote(articleId, value);
    }

    @Transactional
    public ArticleReadingView replaceTags(long articleId, String tagNames) {
        articles.get(articleId);
        var names = new LinkedHashSet<String>();
        Arrays.stream((tagNames == null ? "" : tagNames).split("[,，;；\\r\\n]+"))
                .map(String::trim).filter(value -> !value.isBlank()).forEach(value -> {
                    if (value.length() > 64) {
                        throw new BusinessRuleException("ARTICLE-TAG-LENGTH", "标签名称不能超过 64 个字符");
                    }
                    names.add(value);
                });
        if (names.size() > 20) {
            throw new BusinessRuleException("ARTICLE-TAG-COUNT", "每篇文章最多设置 20 个标签");
        }
        return gateway.replaceTags(articleId, List.copyOf(names));
    }

    @Transactional(readOnly = true)
    public List<TagView> tags() {
        return gateway.tags();
    }
}
