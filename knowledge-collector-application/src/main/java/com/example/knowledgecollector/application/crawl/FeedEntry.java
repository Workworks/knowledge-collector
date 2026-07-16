package com.example.knowledgecollector.application.crawl;

import java.time.OffsetDateTime;

public record FeedEntry(String title, String url, String author, String summary, OffsetDateTime publishedAt) {}
