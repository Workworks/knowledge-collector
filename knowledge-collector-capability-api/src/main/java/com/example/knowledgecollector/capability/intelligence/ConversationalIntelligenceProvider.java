package com.example.knowledgecollector.capability.intelligence;

import java.util.List;

/** 可由 Ollama、OpenAI、DeepSeek 等实现的通用对话能力。 */
public interface ConversationalIntelligenceProvider {
    String id();

    ContentIntelligenceProvider.ProviderStatus status();

    ChatResult chat(ChatRequest request);

    record ChatMessage(String role, String content) {
    }

    record ChatRequest(List<ChatMessage> messages) {
    }

    record ChatResult(String content, String provider, String model,
                      int promptTokens, int responseTokens, long durationMillis) {
    }
}
