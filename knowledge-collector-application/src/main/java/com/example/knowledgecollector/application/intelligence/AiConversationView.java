package com.example.knowledgecollector.application.intelligence;

import java.time.OffsetDateTime;
import java.util.List;

public record AiConversationView(
        Long id, String title, String provider, String model,
        OffsetDateTime createdAt, OffsetDateTime updatedAt,
        List<AiChatMessageView> messages
) {
}
