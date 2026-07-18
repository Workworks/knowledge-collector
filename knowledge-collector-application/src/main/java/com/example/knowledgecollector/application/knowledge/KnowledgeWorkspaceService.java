package com.example.knowledgecollector.application.knowledge;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeWorkspaceService {
    private static final Set<String> CARD_TYPES = Set.of("FACT", "DATA", "DEFINITION", "CONCEPT", "METHOD",
            "CASE", "OPINION", "CONCLUSION", "PREDICTION", "QUOTE", "TO_VERIFY", "PERSONAL_THOUGHT");
    private static final Set<String> RELATIONS = Set.of("SUPPORTS", "OPPOSES", "SUPPLEMENTS", "EXPLAINS",
            "EXAMPLE_OF", "PREREQUISITE", "FOLLOW_UP", "SIMILAR", "CONTRASTS", "CAUSES", "BELONGS_TO",
            "REPLACES", "CONFLICTS");
    private static final Set<String> ENTITY_TYPES = Set.of("PERSON", "COMPANY", "ORGANIZATION", "REGION",
            "PRODUCT", "TECHNOLOGY", "THEORY", "PAPER", "POLICY", "EVENT", "INDUSTRY", "TERM");
    private static final Set<String> EVIDENCE_TYPES = Set.of("SUPPORT", "OPPOSE", "DATA", "CASE", "QUOTE");
    private final KnowledgeWorkspaceGateway gateway;

    public KnowledgeWorkspaceService(KnowledgeWorkspaceGateway gateway) {
        this.gateway = gateway;
    }

    public Map<String, Object> createCard(Map<String, Object> input) {
        require(input, "title", "卡片标题不能为空");
        require(input, "content", "卡片正文不能为空");
        normalized(input, "cardType", CARD_TYPES, "FACT");
        return gateway.createCard(input);
    }

    public int saveAiRecommendedCards(long articleId, Map<String, Object> analysis) {
        Object raw = analysis.get("recommendedCards");
        if (!(raw instanceof List<?> recommendations)) return 0;
        List<Map<String, Object>> existing = new ArrayList<>(gateway.listCards(articleId, null, null));
        int saved = 0;
        for (Object item : recommendations) {
            if (!(item instanceof Map<?, ?> values)) continue;
            Map<String, Object> card = new LinkedHashMap<>();
            values.forEach((key, value) -> card.put(String.valueOf(key), value));
            String title = text(card, "title"); String content = text(card, "content");
            if (title == null || content == null || existing.stream().anyMatch(current ->
                    title.equalsIgnoreCase(String.valueOf(current.get("title"))) && content.equals(current.get("content")))) continue;
            card.put("articleId", articleId); card.put("aiSuggested", true); card.put("confirmed", false);
            try { existing.add(createCard(card)); saved++; } catch (BusinessRuleException ignored) { /* 丢弃格式不合规的 AI 建议 */ }
        }
        return saved;
    }

    public List<Map<String, Object>> cards(Long articleId, String type, Boolean confirmed) {
        return gateway.listCards(articleId, optionalEnum(type, CARD_TYPES), confirmed);
    }

    public Map<String, Object> confirmCard(long id) { return gateway.confirmCard(id); }

    public Map<String, Object> relateCards(long fromId, long toId, Map<String, Object> input) {
        if (fromId == toId) throw rule("KNOWLEDGE-SELF-RELATION", "知识卡片不能关联自身");
        String type = enumValue(input.get("relationType"), RELATIONS, "SIMILAR");
        return gateway.relateCards(fromId, toId, type, text(input, "note"));
    }

    public Map<String, Object> createClaim(Map<String, Object> input) {
        require(input, "statement", "观点内容不能为空");
        return gateway.createClaim(input);
    }
    public List<Map<String, Object>> claims() { return gateway.listClaims(); }
    public Map<String, Object> addEvidence(long claimId, Map<String, Object> input) {
        require(input, "excerpt", "证据摘录不能为空");
        normalized(input, "evidenceType", EVIDENCE_TYPES, "SUPPORT");
        return gateway.addEvidence(claimId, input);
    }

    public Map<String, Object> createEntity(Map<String, Object> input) {
        require(input, "canonicalName", "实体名称不能为空");
        normalized(input, "entityType", ENTITY_TYPES, "TERM");
        return gateway.createEntity(input);
    }
    public List<Map<String, Object>> entities(String type, String keyword) {
        return gateway.listEntities(optionalEnum(type, ENTITY_TYPES), keyword);
    }
    public Map<String, Object> addEntityReference(long id, Map<String, Object> input) {
        return gateway.addEntityReference(id, input);
    }
    public Map<String, Object> mergeEntity(long sourceId, long targetId) {
        if (sourceId == targetId) throw rule("ENTITY-SELF-MERGE", "实体不能合并到自身");
        return gateway.mergeEntity(sourceId, targetId);
    }

    public Map<String, Object> createEvent(Map<String, Object> input) {
        require(input, "title", "事件标题不能为空");
        return gateway.createEvent(input);
    }
    public List<Map<String, Object>> events() { return gateway.listEvents(); }
    public Map<String, Object> attachEventArticle(long eventId, Map<String, Object> input) {
        long articleId = longValue(input, "articleId", true);
        return gateway.attachEventArticle(eventId, articleId, defaultText(input, "sourceRole", "MEDIA"),
                bool(input, "confirmed", false));
    }

    public Map<String, Object> createTopicPage(Map<String, Object> input) {
        require(input, "name", "专题名称不能为空");
        return gateway.createTopicPage(input);
    }
    public List<Map<String, Object>> topicPages() { return gateway.listTopicPages(); }

    public Map<String, Object> createProject(Map<String, Object> input) {
        require(input, "title", "研究项目标题不能为空");
        require(input, "objective", "研究目标不能为空");
        return gateway.createProject(input);
    }
    public List<Map<String, Object>> projects() { return gateway.listProjects(); }
    public Map<String, Object> addProjectItem(long id, Map<String, Object> input) {
        return gateway.addProjectItem(id, input);
    }

    public Map<String, Object> createSynthesis(Map<String, Object> input) {
        require(input, "title", "综合归纳标题不能为空");
        require(input, "content", "综合归纳正文不能为空");
        require(input, "sourceReferences", "综合结论必须关联来源");
        return gateway.createSynthesis(input);
    }
    public List<Map<String, Object>> syntheses() { return gateway.listSyntheses(); }

    public Map<String, Object> createDraft(Map<String, Object> input) {
        require(input, "title", "写作标题不能为空");
        if (text(input, "sourceReferences") == null) input.put("sourceReferences", "[]");
        return gateway.createDraft(input);
    }
    public List<Map<String, Object>> drafts() { return gateway.listDrafts(); }

    public Map<String, Object> createGap(Map<String, Object> input) {
        require(input, "description", "知识缺口描述不能为空");
        return gateway.createGap(input);
    }
    public List<Map<String, Object>> gaps(String status) { return gateway.listGaps(status); }

    public Map<String, Object> scheduleReview(long cardId, Map<String, Object> input) {
        OffsetDateTime due = input.get("dueAt") == null ? OffsetDateTime.now() : OffsetDateTime.parse(input.get("dueAt").toString());
        return gateway.scheduleReview(cardId, defaultText(input, "reviewType", "SPACED"), due);
    }
    public List<Map<String, Object>> dueReviews() { return gateway.dueReviews(OffsetDateTime.now()); }
    public Map<String, Object> completeReview(long id, Map<String, Object> input) {
        int days = input.get("nextReviewDays") instanceof Number n ? Math.max(1, n.intValue()) : 7;
        return gateway.completeReview(id, OffsetDateTime.now().plusDays(days));
    }
    public Map<String, Long> statistics() { return gateway.statistics(); }

    private void require(Map<String, Object> values, String key, String message) {
        if (text(values, key) == null) throw rule("KNOWLEDGE-VALIDATION", message);
    }
    private void normalized(Map<String, Object> values, String key, Set<String> allowed, String fallback) {
        values.put(key, enumValue(values.get(key), allowed, fallback));
    }
    private String optionalEnum(String value, Set<String> allowed) {
        return value == null || value.isBlank() ? null : enumValue(value, allowed, null);
    }
    private String enumValue(Object raw, Set<String> allowed, String fallback) {
        String value = raw == null || raw.toString().isBlank() ? fallback : raw.toString().trim().toUpperCase(Locale.ROOT);
        if (value == null || !allowed.contains(value)) throw rule("KNOWLEDGE-ENUM-INVALID", "不支持的类型：" + value);
        return value;
    }
    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key); return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }
    private String defaultText(Map<String, Object> values, String key, String fallback) {
        String value = text(values, key); return value == null ? fallback : value;
    }
    private long longValue(Map<String, Object> values, String key, boolean required) {
        Object value = values.get(key);
        if (value instanceof Number n) return n.longValue();
        if (value != null) try { return Long.parseLong(value.toString()); } catch (NumberFormatException ignored) { }
        if (required) throw rule("KNOWLEDGE-VALIDATION", key + " 必须是有效编号");
        return 0;
    }
    private boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key); return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }
    private BusinessRuleException rule(String code, String message) { return new BusinessRuleException(code, message); }
}
