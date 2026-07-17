package com.example.knowledgecollector.application.intelligence;

import java.time.OffsetDateTime;

public record AiChatMessageView(
        Long id, Long conversationId, String role, String content, String model,
        Integer promptTokens, Integer responseTokens, Long durationMillis,
        Long savedArticleId, OffsetDateTime createdAt
) {
}
