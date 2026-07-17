package com.example.knowledgecollector.application.intelligence;

import com.example.knowledgecollector.capability.intelligence.ConversationalIntelligenceProvider;

import java.util.List;

public interface AiChatGateway {
    List<AiConversationView> list();

    AiConversationView create(String title, String provider);

    AiConversationView get(long conversationId);

    AiChatMessageView appendUser(long conversationId, String content);

    AiChatMessageView appendAssistant(long conversationId,
                                      ConversationalIntelligenceProvider.ChatResult result);

    AiMaterialView saveMaterial(long messageId, String title);
}
