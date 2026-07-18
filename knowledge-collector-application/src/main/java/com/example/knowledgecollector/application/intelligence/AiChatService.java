package com.example.knowledgecollector.application.intelligence;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.capability.ThirdPartyCapabilityService;
import com.example.knowledgecollector.capability.intelligence.ConversationalIntelligenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiChatService {
    private final AiChatGateway gateway;
    private final List<ConversationalIntelligenceProvider> providers;
    private final String defaultProvider;
    private final int maxHistoryMessages;
    private final ThirdPartyCapabilityService capabilities;

    public AiChatService(AiChatGateway gateway, List<ConversationalIntelligenceProvider> providers,
                         @Value("${knowledge-collector.ai.provider:ollama}") String defaultProvider,
                         @Value("${knowledge-collector.ai.chat.max-history-messages:20}") int maxHistoryMessages,
                         ThirdPartyCapabilityService capabilities) {
        this.gateway = gateway;
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.maxHistoryMessages = Math.max(2, maxHistoryMessages);
        this.capabilities = capabilities;
    }

    public List<AiConversationView> list() {
        return gateway.list();
    }

    public AiConversationView create(String title, String providerId) {
        String selected = providerId == null || providerId.isBlank() ? capabilities.defaultProvider("AI") : providerId;
        provider(selected);
        return gateway.create(title == null || title.isBlank() ? "新对话" : title.trim(), selected);
    }

    public AiConversationView get(long conversationId) {
        return gateway.get(conversationId);
    }

    public AiChatMessageView send(long conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessRuleException("AI-CHAT-CONTENT-EMPTY", "聊天内容不能为空");
        }
        AiConversationView conversation = gateway.get(conversationId);
        ConversationalIntelligenceProvider provider = provider(conversation.provider());
        gateway.appendUser(conversationId, content.trim());
        List<AiChatMessageView> messages = gateway.get(conversationId).messages();
        List<ConversationalIntelligenceProvider.ChatMessage> history = messages
                .stream()
                .skip(Math.max(0, messages.size() - maxHistoryMessages))
                .map(message -> new ConversationalIntelligenceProvider.ChatMessage(
                        message.role().toLowerCase(), message.content()))
                .toList();
        try {
            var request = new ConversationalIntelligenceProvider.ChatRequest(history);
            var result = capabilities.invoke(provider.id(), "AI_CHAT", "AI 研究助手",
                    "conversationId=" + conversationId, () -> provider.chat(request), value -> "AI 回复已生成");
            return gateway.appendAssistant(conversationId, result);
        } catch (Exception exception) {
            throw new BusinessRuleException("AI-CHAT-FAILED",
                    exception.getMessage() == null ? "AI 对话失败" : exception.getMessage());
        }
    }

    public AiMaterialView saveMaterial(long messageId, String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessRuleException("AI-MATERIAL-TITLE-EMPTY", "资料标题不能为空");
        }
        return gateway.saveMaterial(messageId, title.trim());
    }

    private ConversationalIntelligenceProvider provider(String id) {
        return providers.stream().filter(candidate -> candidate.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("AI-PROVIDER-NOT-AVAILABLE",
                        "未找到 AI 对话 Provider：" + id));
    }
}
