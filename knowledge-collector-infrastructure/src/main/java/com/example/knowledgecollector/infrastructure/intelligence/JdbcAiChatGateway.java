package com.example.knowledgecollector.infrastructure.intelligence;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.intelligence.AiChatGateway;
import com.example.knowledgecollector.application.intelligence.AiChatMessageView;
import com.example.knowledgecollector.application.intelligence.AiConversationView;
import com.example.knowledgecollector.application.intelligence.AiMaterialView;
import com.example.knowledgecollector.capability.intelligence.ConversationalIntelligenceProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

@Repository
public class JdbcAiChatGateway implements AiChatGateway {
    private final JdbcClient jdbc;

    public JdbcAiChatGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AiConversationView> list() {
        return jdbc.sql("select * from ai_conversation order by updated_at desc,id desc")
                .query((rs, row) -> conversation(rs, List.of())).list();
    }

    @Override
    @Transactional
    public AiConversationView create(String title, String provider) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("insert into ai_conversation(title,provider,created_at,updated_at) "
                        + "values(:title,:provider,:now,:now)")
                .param("title", title).param("provider", provider).param("now", now).update();
        long id = jdbc.sql("select max(id) from ai_conversation").query(Long.class).single();
        return get(id);
    }

    @Override
    public AiConversationView get(long conversationId) {
        List<AiChatMessageView> messages = jdbc.sql(
                        "select * from ai_chat_message where conversation_id=:id order by id")
                .param("id", conversationId).query(this::message).list();
        return jdbc.sql("select * from ai_conversation where id=:id")
                .param("id", conversationId)
                .query((rs, row) -> conversation(rs, messages)).optional()
                .orElseThrow(() -> new ResourceNotFoundException("AI 对话不存在：" + conversationId));
    }

    @Override
    @Transactional
    public AiChatMessageView appendUser(long conversationId, String content) {
        AiConversationView conversation = get(conversationId);
        OffsetDateTime now = OffsetDateTime.now();
        insertMessage(conversationId, "USER", content, null, null, null, null, now);
        String title = conversation.messages().isEmpty() && "新对话".equals(conversation.title())
                ? abbreviate(content.replaceAll("\\s+", " "), 60) : conversation.title();
        jdbc.sql("update ai_conversation set title=:title,updated_at=:now where id=:id")
                .param("title", title).param("now", now).param("id", conversationId).update();
        return latest(conversationId);
    }

    @Override
    @Transactional
    public AiChatMessageView appendAssistant(long conversationId,
                                              ConversationalIntelligenceProvider.ChatResult result) {
        get(conversationId);
        OffsetDateTime now = OffsetDateTime.now();
        insertMessage(conversationId, "ASSISTANT", result.content(), result.model(),
                result.promptTokens(), result.responseTokens(), result.durationMillis(), now);
        jdbc.sql("update ai_conversation set model=:model,updated_at=:now where id=:id")
                .param("model", result.model()).param("now", now).param("id", conversationId).update();
        return latest(conversationId);
    }

    @Override
    @Transactional
    public AiMaterialView saveMaterial(long messageId, String title) {
        MaterialMessage message = jdbc.sql("""
                select m.id,m.conversation_id,m.content,m.model,m.saved_article_id,c.provider
                from ai_chat_message m join ai_conversation c on c.id=m.conversation_id
                where m.id=:id and m.role='ASSISTANT'
                """).param("id", messageId).query((rs, row) -> new MaterialMessage(
                        rs.getLong("id"), rs.getLong("conversation_id"), rs.getString("content"),
                        rs.getString("model"), (Long) rs.getObject("saved_article_id"),
                        rs.getString("provider"))).optional()
                .orElseThrow(() -> new BusinessRuleException("AI-MATERIAL-NOT-AVAILABLE",
                        "只能保存 AI 回复内容"));
        if (message.savedArticleId() != null) {
            return new AiMaterialView(message.savedArticleId(), title, "PENDING_REVIEW", "AI_GENERATED");
        }
        long sourceId = jdbc.sql("select id from crawl_source where source_code='SYSTEM_AI'")
                .query(Long.class).single();
        OffsetDateTime now = OffsetDateTime.now();
        String localUrl = "http://127.0.0.1/ai-chat/conversations/" + message.conversationId()
                + "/messages/" + message.id();
        String hash = sha256(localUrl);
        String fingerprint = sha256(message.content());
        int words = wordCount(message.content());
        jdbc.sql("""
                insert into article(source_id,title,author,summary,original_url,normalized_url,url_hash,
                language,publish_time,publish_time_inferred,content_text,word_count,reading_minutes,
                content_fingerprint,review_status,source_level,content_origin,ai_conversation_id,
                ai_message_id,first_collected_at,last_collected_at,created_at,updated_at)
                values(:sourceId,:title,:author,:summary,:url,:url,:hash,'zh-CN',:now,false,:content,
                :words,:minutes,:fingerprint,'PENDING_REVIEW','AI_GENERATED','AI_GENERATED',
                :conversationId,:messageId,:now,:now,:now,:now)
                """).param("sourceId", sourceId).param("title", title)
                .param("author", firstNonBlank(message.model(), message.provider(), "AI"))
                .param("summary", abbreviate(message.content(), 300)).param("url", localUrl)
                .param("hash", hash).param("content", message.content()).param("words", words)
                .param("minutes", words == 0 ? 0 : Math.max(1, (int) Math.ceil(words / 250.0)))
                .param("fingerprint", fingerprint).param("conversationId", message.conversationId())
                .param("messageId", message.id()).param("now", now).update();
        long articleId = jdbc.sql("select id from article where url_hash=:hash")
                .param("hash", hash).query(Long.class).single();
        jdbc.sql("update ai_chat_message set saved_article_id=:articleId where id=:id")
                .param("articleId", articleId).param("id", messageId).update();
        return new AiMaterialView(articleId, title, "PENDING_REVIEW", "AI_GENERATED");
    }

    private void insertMessage(long conversationId, String role, String content, String model,
                               Integer promptTokens, Integer responseTokens, Long durationMillis,
                               OffsetDateTime now) {
        jdbc.sql("""
                insert into ai_chat_message(conversation_id,role,content,model,prompt_tokens,
                response_tokens,duration_millis,created_at)
                values(:conversationId,:role,:content,:model,:promptTokens,:responseTokens,:duration,:now)
                """).param("conversationId", conversationId).param("role", role)
                .param("content", content).param("model", model).param("promptTokens", promptTokens)
                .param("responseTokens", responseTokens).param("duration", durationMillis)
                .param("now", now).update();
    }

    private AiChatMessageView latest(long conversationId) {
        return jdbc.sql("select * from ai_chat_message where conversation_id=:id order by id desc limit 1")
                .param("id", conversationId).query(this::message).single();
    }

    private AiConversationView conversation(ResultSet rs, List<AiChatMessageView> messages) throws SQLException {
        return new AiConversationView(rs.getLong("id"), rs.getString("title"), rs.getString("provider"),
                rs.getString("model"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class), messages);
    }

    private AiChatMessageView message(ResultSet rs, int row) throws SQLException {
        return new AiChatMessageView(rs.getLong("id"), rs.getLong("conversation_id"),
                rs.getString("role"), rs.getString("content"), rs.getString("model"),
                (Integer) rs.getObject("prompt_tokens"), (Integer) rs.getObject("response_tokens"),
                (Long) rs.getObject("duration_millis"), (Long) rs.getObject("saved_article_id"),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private int wordCount(String text) {
        return text == null || text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 AI 资料指纹", exception);
        }
    }

    private String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "AI";
    }

    private record MaterialMessage(long id, long conversationId, String content, String model,
                                   Long savedArticleId, String provider) {
    }
}
